package org.multiverse.stms.beta.integrationtest.liveness;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;

import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;

public class PrivatizationDeadLockStressTest {

    private int threadCount = 4;
    private int refCount = 5;
    private volatile boolean stop;
    private BetaStm stm;
    private BetaLongRef[] refs;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = new BetaStm();
        stop = false;

        refs = new BetaLongRef[refCount];
        for (int k = 0; k < refCount; k++) {
            refs[k] = newLongRef(stm);
        }
    }

    @Test
    public void test() {
        StressThread[] threads = new StressThread[threadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new StressThread(k);
        }

        startAll(threads);
        sleepMs(getStressTestDurationMs(60 * 1000));
        stop = true;
        joinAll(threads);
    }

    class StressThread extends TestThread {
        private boolean leftToRight;

        public StressThread(int id) {
            super("StressThread-" + id);
            this.leftToRight = id % 2 == 0;
        }

        @Override
        public void doRun() throws Exception {
            AtomicBlock block = stm.createTransactionFactoryBuilder()
                    .setSpinCount(1000)
                    .setMaxRetries(10000)
                    .buildAtomicBlock();

            AtomicVoidClosure closure = new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    if (leftToRight) {
                        for (int k = 0; k < refCount; k++) {
                            refs[k].privatize(tx);
                            sleepMs(5);
                        }
                    } else {
                        for (int k = refCount - 1; k >= 0; k--) {
                            refs[k].privatize(tx);
                            sleepMs(5);
                        }
                    }
                }
            };

            int k = 0;
            while (!stop) {
                block.execute(closure);
                sleepMs(10);
                k++;

                if (k % 10 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }
        }
    }
}
