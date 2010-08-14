package org.multiverse.stms.beta.integrationtest.isolation;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.TestUtils;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaTransactionFactory;
import org.multiverse.stms.beta.BetaTransactionTemplate;
import org.multiverse.stms.beta.refs.IntRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.createIntRef;
import static org.multiverse.stms.beta.ThreadLocalBetaObjectPool.getThreadLocalBetaObjectPool;

public class DirtyReadStressTest {
    private IntRef ref;

    private int readThreadCount = 10;
    private int modifyThreadCount = 2;

    private volatile boolean stop;
    private BetaStm stm;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = new BetaStm();
        ref = createIntRef(stm);
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
        sleepMs(TestUtils.getStressTestDurationMs(30 * 1000));
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
            while (!stop) {
                try {
                    modify();
                    fail();
                } catch (DeadTransactionException ignore) {
                }

                sleepRandomMs(10);
            }
        }

        private void modify() {
            new BetaTransactionTemplate(stm){
                @Override
                public Object execute(BetaTransaction tx) throws Exception {
                    BetaObjectPool pool = getThreadLocalBetaObjectPool();
                    ref.set(tx, pool, ref.get(tx, pool));
                    tx.abort(pool);
                    return null;
                }
            }.execute();
        }
    }

    class ReadThread extends TestThread {
        public ReadThread(int threadId) {
            super("ReadThread-" + threadId);
        }

        @Override
        public void doRun() {
            int k = 0;
            while (!stop) {
                switch (k% 2){
                    case 0:
                        observeUsingReadtrackingReadonlyTransaction();
                        break;
                    case 1:
                    case 3:
                        observeUsingReadtrackingUpdateTransaction();
                        break;
                    default:
                        throw new IllegalStateException();
                }

                k++;
                sleepRandomMs(5);
            }
        }

        private void _observeUsingReadonlyTransaction() {
            BetaTransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                    .setReadonly(false)
                    .setReadTrackingEnabled(true)
                    .build();


            new BetaTransactionTemplate(txFactory){
                @Override
                public Object execute(BetaTransaction tx) throws Exception {
                    observe(tx, getThreadLocalBetaObjectPool());
                    return null;
                }
            }.execute();
        }


        private void _observeUsingUpdateTransaction() {
            BetaTransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                    .setReadonly(false)
                    .setReadTrackingEnabled(true)
                    .build();


            new BetaTransactionTemplate(txFactory){
                @Override
                public Object execute(BetaTransaction tx) throws Exception {
                    observe(tx, getThreadLocalBetaObjectPool());
                    return null;
                }
            }.execute();
        }

        private void observeUsingReadtrackingReadonlyTransaction() {
            BetaTransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                    .setReadonly(true)
                    .setReadTrackingEnabled(true)
                    .build();

            new BetaTransactionTemplate(txFactory){
                @Override
                public Object execute(BetaTransaction tx) throws Exception {
                    observe(tx, getThreadLocalBetaObjectPool());
                    return null;
                }
            }.execute();
        }


        private void observeUsingReadtrackingUpdateTransaction() {
            BetaTransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                    .setReadonly(false)
                    .setReadTrackingEnabled(true)
                    .build();

            new BetaTransactionTemplate(txFactory){
                @Override
                public Object execute(BetaTransaction tx) throws Exception {
                    observe(tx, getThreadLocalBetaObjectPool());
                    return null;
                }
            }.execute();
        }

        private void observe(BetaTransaction tx, BetaObjectPool pool) {
            if (ref.get(tx, pool) % 2 != 0) {
                fail();
            }
        }
    }
}
