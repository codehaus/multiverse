package org.multiverse.stms.beta.integrationtest.failureatomicity;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.exceptions.TooManyRetriesException;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaTransactionFactory;
import org.multiverse.stms.beta.BetaTransactionTemplate;
import org.multiverse.stms.beta.refs.IntRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.createIntRef;
import static org.multiverse.stms.beta.ThreadLocalBetaObjectPool.getThreadLocalBetaObjectPool;

public class TooManyRetriesRollbackTest {
    private IntRef modifyRef;
    private IntRef retryRef;
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
        assertEquals(0, modifyRef.unsafeLoad().value);
        joinAll(notifyThread);
    }


    public void setAndAwaitUneven(final int value) {
        BetaTransactionFactory txFactory =stm.getTransactionFactoryBuilder()
                .setMaxRetries(10)
                .build();

        new BetaTransactionTemplate(txFactory) {
            @Override
            public Object execute(BetaTransaction tx) throws Exception {
                BetaObjectPool pool = getThreadLocalBetaObjectPool();

                modifyRef.set(tx, pool, value);

                if (retryRef.get(tx, pool) % 2 == 0) {
                    retry();
                }

                return null;
            }
        }.execute();

    }

    class NotifyThread extends TestThread {

        public NotifyThread() {
            super("NotifyThread");
        }

        @Override
        public void doRun() throws Exception {
            while (!finished) {
                new BetaTransactionTemplate(stm){
                    @Override
                    public Object execute(BetaTransaction tx) throws Exception {
                        BetaObjectPool pool = getThreadLocalBetaObjectPool();

                        int value = retryRef.get(tx, pool);
                        retryRef.set(tx, pool, value+2);
                        return null;
                    }
                }.execute();
            }
        }
    }

}
