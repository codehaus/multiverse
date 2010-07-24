package org.multiverse.stms.beta.integrationtest;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.TransactionFactory;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.ObjectPool;
import org.multiverse.stms.beta.TransactionTemplate;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.LongRefTranlocal;
import org.multiverse.stms.beta.transactions.ArrayBetaTransaction;
import org.multiverse.stms.beta.transactions.ArrayTreeBetaTransaction;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.MonoBetaTransaction;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.stms.beta.StmUtils.createLongRef;
import static org.multiverse.TestUtils.sleepMs;

public class PingPingStressTest {

    private volatile boolean stop = false;
    private int threadCount = 2;
    private LongRef ref;
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        ref = createLongRef(stm);
        stop = false;
    }

    @Test
    public void whenMonoTransaction() throws InterruptedException {
        test(new TransactionFactory() {
            @Override
            public BetaTransaction start() {
                return new MonoBetaTransaction(stm);
            }
        });
    }


    @Test
    public void whenArrayTransaction() throws InterruptedException {
        test(new TransactionFactory() {
            @Override
            public BetaTransaction start() {
                return new ArrayBetaTransaction(stm,1);
            }
        });
    }

    @Test
    public void whenArrayTreeTransaction() throws InterruptedException {
        test(new TransactionFactory() {
            @Override
            public BetaTransaction start() {
                return new ArrayTreeBetaTransaction(stm);
            }
        });
    }

    public void test(TransactionFactory transactionFactory) throws InterruptedException {
        PingPongThread[] threads = createThreads(transactionFactory);

        for (PingPongThread thread : threads) {
            thread.start();
        }

        sleepMs(30 * 1000);
        stop = true;

        System.out.println("Waiting for joining threads");
        for (PingPongThread thread : threads) {
            thread.join();
        }

        assertEquals(sum(threads), ref.unsafeLoad().value);
    }

    private PingPongThread[] createThreads(TransactionFactory transactionFactory) {
        PingPongThread[] threads = new PingPongThread[threadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new PingPongThread(k, transactionFactory.start());
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

    private class PingPongThread extends Thread {
        private final int id;
        private long count;
        private final BetaTransaction tx;

        public PingPongThread(int id, BetaTransaction tx) {
            super("PingPongThread-" + id);
            this.id = id;
            this.tx = tx;
        }

        @Override
        public void run() {

            final ObjectPool pool = new ObjectPool();

            TransactionTemplate t = new TransactionTemplate(stm, tx) {
                @Override
                public void execute(BetaTransaction tx) {
                    LongRefTranlocal write = tx.openForWrite(ref, false, pool);

                    if (write.value % threadCount != id) {
                        retry();
                    }

                    write.value++;
                    //           System.out.println(getName()+" writing.value:"+write.value);
                }
            };

            while (!stop) {
                if (count % (100 * 1000) == 0) {
                    System.out.println(getName() + " " + count);
                }

                t.execute(pool);
                count++;
            }
        }
    }
}
