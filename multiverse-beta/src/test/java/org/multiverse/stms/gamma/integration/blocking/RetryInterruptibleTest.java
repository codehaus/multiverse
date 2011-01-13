package org.multiverse.stms.gamma.integration.blocking;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.exceptions.RetryInterruptedException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaIntRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertTrue;
import static org.multiverse.TestUtils.assertAlive;
import static org.multiverse.TestUtils.sleepMs;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmTestUtils.newIntRef;

public class RetryInterruptibleTest {

    private BetaIntRef ref;
    private BetaStm stm;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = (BetaStm) getGlobalStmInstance();
        ref = newIntRef(stm);
    }

    @Test
    public void test() throws InterruptedException {
        ref = new BetaIntRef(stm, 0);

        AwaitThread t = new AwaitThread();
        t.start();

        sleepMs(200);
        assertAlive(t);
        t.interrupt();

        t.join();
        assertTrue(t.wasInterrupted);
    }

    class AwaitThread extends TestThread {
        private boolean wasInterrupted;

        public void doRun() throws Exception {
            try {
                await();
            } catch (RetryInterruptedException e) {
                wasInterrupted = true;
            }
        }

        public void await() throws Exception {
            AtomicBlock block = stm.createTransactionFactoryBuilder()
                    .setInterruptible(true)
                    .buildAtomicBlock();

            block.executeChecked(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    if (ref.get(btx) != 1) {
                        retry();
                    }
                }
            });
        }
    }
}
