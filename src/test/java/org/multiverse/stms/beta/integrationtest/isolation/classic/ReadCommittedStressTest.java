package org.multiverse.stms.beta.integrationtest.isolation.classic;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmUtils;
import org.multiverse.stms.beta.transactionalobjects.BetaIntRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmTestUtils.newIntRef;

/**
 * A Stresstest that check if the it is possible that a dirty read is done (this is not allowed).
 *
 * @author Peter Veentjer.
 */
public class ReadCommittedStressTest {
    private BetaIntRef ref;

    private int readThreadCount = 10;
    private int modifyThreadCount = 2;

    private volatile boolean stop;
    private BetaStm stm;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = new BetaStm();
        ref = newIntRef(stm);
        stop = false;
    }

    @Test
    public void test() {
        FailingModifyThread[] modifyThreads = new FailingModifyThread[modifyThreadCount];
        for (int k = 0; k < modifyThreadCount; k++) {
            modifyThreads[k] = new FailingModifyThread(k);
        }

        ReadThread[] readerThread = new ReadThread[readThreadCount];
        for (int k = 0; k < readThreadCount; k++) {
            readerThread[k] = new ReadThread(k);
        }

        startAll(modifyThreads);
        startAll(readerThread);
        sleepMs(getStressTestDurationMs(30 * 1000));
        stop = true;
        joinAll(modifyThreads);
        joinAll(readerThread);
    }

    class FailingModifyThread extends TestThread {

        public FailingModifyThread(int threadId) {
            super("FailingModifyThread-" + threadId);
        }

        @Override
        public void doRun() {
            AtomicBlock block = stm.getDefaultAtomicBlock();
            AtomicVoidClosure closure = new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    ref.getAndSet(btx, ref.get(btx));
                    btx.abort();
                }
            };

            while (!stop) {
                try {
                    block.execute(closure);
                    fail();
                } catch (DeadTransactionException ignore) {
                }

                sleepRandomMs(10);
            }
        }
    }

    class ReadThread extends TestThread {


        public ReadThread(int threadId) {
            super("ReadThread-" + threadId);
        }

        @Override
        public void doRun() {
            AtomicVoidClosure closure = new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;

                    if (ref.get(btx) % 2 != 0) {
                        fail();
                    }
                }
            };

            AtomicBlock readonlyReadtrackingBlock = stm.createTransactionFactoryBuilder()
                    .setReadonly(true)
                    .setReadTrackingEnabled(true)
                    .buildAtomicBlock();

            AtomicBlock updateReadtrackingBlock = stm.createTransactionFactoryBuilder()
                    .setReadonly(false)
                    .setReadTrackingEnabled(true)
                    .buildAtomicBlock();

            int k = 0;
            while (!stop) {
                switch (k % 2) {
                    case 0:
                        readonlyReadtrackingBlock.execute(closure);
                        break;
                    case 1:
                    case 3:
                        updateReadtrackingBlock.execute(closure);
                        break;
                    default:
                        throw new IllegalStateException();
                }

                k++;
                sleepRandomMs(5);
            }
        }
    }
}
