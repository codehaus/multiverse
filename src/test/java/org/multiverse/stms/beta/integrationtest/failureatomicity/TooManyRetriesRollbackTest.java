package org.multiverse.stms.beta.integrationtest.failureatomicity;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.exceptions.TooManyRetriesException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaIntRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.createIntRef;

public class TooManyRetriesRollbackTest {
    private BetaIntRef modifyRef;
    private BetaIntRef retryRef;
    private volatile boolean finished;
    private BetaStm stm;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = new BetaStm();
        modifyRef = createIntRef(stm);
        retryRef = createIntRef(stm);
        finished = false;
    }

    @Test
    public void test() {
        NotifyThread notifyThread = new NotifyThread();
        notifyThread.start();

        try {
            setAndAwaitUneven(1);
            fail();
        } catch (TooManyRetriesException expected) {
        }

        finished = true;
        assertEquals(0, modifyRef.___unsafeLoad().value);
        joinAll(notifyThread);
    }


    public void setAndAwaitUneven(final int value) {
        AtomicBlock block = stm.createTransactionFactoryBuilder()
                .setMaxRetries(10)
                .buildAtomicBlock();

        block.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                BetaTransaction btx = (BetaTransaction) tx;

                modifyRef.set(btx, value);

                if (retryRef.get(btx) % 2 == 0) {
                    retry();
                }
            }
        });
    }

    class NotifyThread extends TestThread {

        public NotifyThread() {
            super("NotifyThread");
        }

        @Override
        public void doRun() throws Exception {
            AtomicBlock block = stm.createTransactionFactoryBuilder()
                    .buildAtomicBlock();
            AtomicVoidClosure closure = new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;

                    int value = retryRef.get(btx);
                    retryRef.set(btx, value + 2);
                }
            };

            while (!finished) {
                block.execute(closure);
            }
        }
    }
}
