package org.multiverse.stms.beta.integrationtest.traditionalsynchronization;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.TestUtils;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaIntRef;
import org.multiverse.stms.beta.transactionalobjects.IntRefTranlocal;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.multiverse.TestUtils.*;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.createIntRef;
import static org.multiverse.stms.beta.ThreadLocalBetaObjectPool.getThreadLocalBetaObjectPool;

/**
 * A StressTest that checks if a the Semaphore; a traditional synchronization structure can be build
 * using an STM.
 */
public class NonReentrantSemaphoreStressTest {
    private BetaStm stm;
    private boolean pessimistic;
    private volatile boolean stop;
    private int threadCount = 10;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = new BetaStm();
        stop = false;
    }

    @Test
    public void testPessimistic() {
        test(true);
    }

    @Test
    public void testOptimistic() {
        test(false);
    }

    public void test(boolean pessimistic) {
        this.pessimistic = pessimistic;

        WorkerThread[] threads = new WorkerThread[threadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new WorkerThread(k);
        }

        startAll(threads);
        sleepMs(TestUtils.getStressTestDurationMs(60 * 1000));
        stop = true;
        joinAll(threads);

        //assertEquals(sum(threads), sum(intValues));
        //System.out.println("total increments: " + sum(threads));
    }

    class WorkerThread extends TestThread {
        public WorkerThread(int id) {
            super("Worker-" + id);
        }

        @Override
        public void doRun() throws Exception {
            while (!stop) {

            }
        }
    }

    class Semaphore {

        private final BetaIntRef ref;
        private final AtomicBlock upBlock = stm.getTransactionFactoryBuilder().buildAtomicBlock();
        private final AtomicBlock downBlock = stm.getTransactionFactoryBuilder().buildAtomicBlock();
        private final AtomicVoidClosure upClosure = new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                BetaTransaction btx = (BetaTransaction) tx;
                BetaObjectPool pool = getThreadLocalBetaObjectPool();

                IntRefTranlocal write = btx.openForWrite(ref, pessimistic, pool);
                write.value++;
            }
        };
        private final AtomicVoidClosure downClosure = new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                BetaTransaction btx = (BetaTransaction) tx;
                BetaObjectPool pool = getThreadLocalBetaObjectPool();

                IntRefTranlocal write = btx.openForWrite(ref, pessimistic, pool);
                if (write.value == 0) {
                    retry();
                }

                write.value--;
            }
        };

        public Semaphore(int initial) {
            ref = createIntRef(stm, initial);
        }

        public void up() {
            upBlock.execute(upClosure);
        }

        public void down() {
            downBlock.execute(downClosure);
        }
    }
}
