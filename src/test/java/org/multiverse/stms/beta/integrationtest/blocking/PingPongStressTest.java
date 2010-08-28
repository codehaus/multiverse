package org.multiverse.stms.beta.integrationtest.blocking;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicBooleanClosure;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.stms.beta.*;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static java.lang.Math.abs;
import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

public class PingPongStressTest {

    private volatile boolean stop = false;
    private BetaLongRef ref;
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
        AtomicBlock block = new LeanBetaAtomicBlock(transactionFactory);
        PingPongThread[] threads = createThreads(block, threadCount);

        startAll(threads);

        sleepMs(30 * 1000);
        stop = true;

        final BetaObjectPool pool = new BetaObjectPool();

        stm.getDefaultAtomicBlock().execute(new AtomicVoidClosure(){
            @Override
            public void execute(Transaction tx) throws Exception {
                BetaTransaction btx = (BetaTransaction)tx;
                LongRefTranlocal write = btx.openForWrite(ref, false, pool);
                write.value = -abs(write.value);
            }
        });

        System.out.println("Waiting for joining threads");
        joinAll(threads);

        assertEquals(sum(threads), -ref.___unsafeLoad().value);
    }

    private PingPongThread[] createThreads(AtomicBlock block, int threadCount) {
        PingPongThread[] threads = new PingPongThread[threadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new PingPongThread(k, block, threadCount);
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
        private final AtomicBlock block;
        private final int threadCount;
        private final int id;
        private long count;

        public PingPongThread(int id, AtomicBlock block, int threadCount) {
            super("PingPongThread-" + id);
            this.id = id;
            this.block = block;
            this.threadCount = threadCount;
        }

        @Override
        public void doRun() {
            final BetaObjectPool pool = new BetaObjectPool();

            AtomicBooleanClosure closure = new AtomicBooleanClosure() {
                @Override
                public boolean execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    LongRefTranlocal write = btx.openForWrite(ref, false, pool);

                    if (write.value < 0) {
                        return false;
                    }

                    if (write.value % threadCount != id) {
                        retry();
                    }

                    write.value++;
                    return true;
                }
            };

            while (!stop) {
                if (count % (20000) == 0) {
                    System.out.println(getName() + " " + count);
                }

                if(!block.execute(closure)){
                    break;
                }
                count++;
            }

            System.out.printf("%s finished\n", getName());
        }
    }
}
