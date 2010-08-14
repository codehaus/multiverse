package org.multiverse.stms.beta.integrationtest.isolation;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.TestUtils;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaTransactionFactory;
import org.multiverse.stms.beta.BetaTransactionTemplate;
import org.multiverse.stms.beta.refs.IntRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.createIntRef;
import static org.multiverse.stms.beta.ThreadLocalBetaObjectPool.getThreadLocalBetaObjectPool;

public class ReadonlyRepeatableReadStressTest {

    private volatile boolean stop;
    private IntRef ref;
    private int readThreadCount = 5;
    private int modifyThreadCount = 2;
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
        ModifyThread[] modifyThreads = new ModifyThread[modifyThreadCount];
        for (int k = 0; k < modifyThreadCount; k++) {
            modifyThreads[k] = new ModifyThread(k);
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

    class ModifyThread extends TestThread {

        public ModifyThread(int id) {
            super("ModifyThread-" + id);
        }

        @Override
        public void doRun() {
            while (!stop) {
                new BetaTransactionTemplate(stm){
                    @Override
                    public Object execute(BetaTransaction tx) throws Exception {
                        BetaObjectPool pool = getThreadLocalBetaObjectPool();
                        ref.set(tx,pool, ref.get(tx,pool));
                        return null;
                    }
                }.execute();
                sleepRandomMs(5);
            }
        }
    }

    class ReadThread extends TestThread {

        public ReadThread(int id) {
            super("ReadThread-" + id);
        }

        @Override
        public void doRun() {
            int k=0;
            while(!stop){
                switch (k % 2){
                    case 0:
                        readUsingReadtrackingReadonlyTransaction();
                        break;
                    case 1:
                        readUsingReadtrackingUpdateTransaction();
                        break;
                    default:
                        throw new IllegalStateException();
                }
                k++;
            }
        }

//        @TransactionalMethod(readonly = true, trackReads = false)
//        private void readUsingReadonlyTransaction() {
//            read();
//        }

//        @TransactionalMethod(readonly = false,trackReads = false)
//        private void readUsingUpdateTransaction() {
//            read();
//        }

        private void readUsingReadtrackingReadonlyTransaction() {
            BetaTransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                    .setReadonly(true)
                    .setReadTrackingEnabled(true)
                    .build();

            new BetaTransactionTemplate(txFactory){
                @Override
                public Object execute(BetaTransaction tx) throws Exception {
                    BetaObjectPool pool = getThreadLocalBetaObjectPool();
                    read(tx,pool);
                    return null;
                }
            }.execute();
        }

        private void readUsingReadtrackingUpdateTransaction() {
             BetaTransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                    .setReadonly(false)
                    .setReadTrackingEnabled(true)
                    .build();

            new BetaTransactionTemplate(txFactory){
                @Override
                public Object execute(BetaTransaction tx) throws Exception {
                    BetaObjectPool pool = getThreadLocalBetaObjectPool();
                    read(tx,pool);
                    return null;
                }
            }.execute();
        }

        private void read(BetaTransaction tx,BetaObjectPool pool) {
            int firstTime = ref.get(tx,pool);
            sleepRandomMs(2);
            int secondTime = ref.get(tx,pool);
            assertEquals(firstTime, secondTime);
        }
    }
}
