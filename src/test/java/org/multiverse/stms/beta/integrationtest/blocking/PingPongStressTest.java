package org.multiverse.stms.beta.integrationtest.blocking;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.stms.beta.*;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.LongRefTranlocal;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static java.lang.Math.abs;
import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

public class PingPongStressTest {

    private volatile boolean stop = false;
    private LongRef ref;
    private BetaStm stm;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = new BetaStm();
        ref = createLongRef(stm);
        stop = false;
    }

    @Test
    public void withMonoTransactionAnd2Threads() throws InterruptedException {
        test(new FatMonoBetaTransactionFactory(stm), 2);
    }

    @Test
    public void withArrayTransactionAnd2Threads() throws InterruptedException {
        test(new FatArrayBetaTransactionFactory(stm), 2);
    }

    @Test
    public void withArrayTreeTransactionAnd2Threads() throws InterruptedException {
        test(new FatArrayTreeBetaTransactionFactory(stm), 2);
    }

    @Test
    public void withMonoTransactionAnd10Threads() throws InterruptedException {
        test(new FatMonoBetaTransactionFactory(stm), 10);
    }

    @Test
    public void withArrayTransactionAnd10Threads() throws InterruptedException {
        test(new FatArrayBetaTransactionFactory(stm), 10);
    }

    @Test
    public void withArrayTreeTransactionAnd10Threads() throws InterruptedException {
        test(new FatArrayTreeBetaTransactionFactory(stm), 10);
    }

    public void test(BetaTransactionFactory transactionFactory, int threadCount) throws InterruptedException {
        PingPongThread[] threads = createThreads(transactionFactory, threadCount);

        startAll(threads);

        sleepMs(30 * 1000);
        stop = true;

        final BetaObjectPool pool = new BetaObjectPool();

        new BetaTransactionTemplate(stm) {
            @Override
            public Object execute(BetaTransaction tx) {
                LongRefTranlocal write = tx.openForWrite(ref, false, pool);
                write.value = -abs(write.value);
                return null;
            }
        }.execute(pool);

        System.out.println("Waiting for joining threads");
        joinAll(threads);

        assertEquals(sum(threads), -ref.unsafeLoad().value);
    }

    private PingPongThread[] createThreads(BetaTransactionFactory transactionFactory, int threadCount) {
        PingPongThread[] threads = new PingPongThread[threadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new PingPongThread(k, transactionFactory, threadCount);
        }
        return threads;
    }

    private long sum(PingPongThread[] threads) {
        long result = 0;
        for (PingPongThread t : threads) {
            result += t.count;
        }
        return result;
    }

    private class PingPongThread extends TestThread {
        private final BetaTransactionFactory txFactory;
        private final int threadCount;
        private final int id;
        private long count;

        public PingPongThread(int id, BetaTransactionFactory txFactory, int threadCount) {
            super("PingPongThread-" + id);
            this.id = id;
            this.txFactory = txFactory;
            this.threadCount = threadCount;
        }

        @Override
        public void doRun() {
            try {
                final BetaObjectPool pool = new BetaObjectPool();

                BetaTransactionTemplate t = new BetaTransactionTemplate(txFactory) {
                    @Override
                    public Object execute(BetaTransaction tx) {
                        LongRefTranlocal write = tx.openForWrite(ref, false, pool);

                        if (write.value < 0) {
                            throw new RuntimeException();
                        }

                        if (write.value % threadCount != id) {
                            retry();
                        }

                        write.value++;
                        return null;
                    }
                };

                while (!stop) {
                    if (count % (20000) == 0) {
                        System.out.println(getName() + " " + count);
                    }

                    try {
                        t.execute(pool);
                    } catch (RuntimeException e) {
                        break;
                    }

                    count++;
                }

                System.out.printf("%s finished\n", getName());
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

}
