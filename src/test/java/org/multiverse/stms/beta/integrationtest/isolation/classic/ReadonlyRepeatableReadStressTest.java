package org.multiverse.stms.beta.integrationtest.isolation.classic;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.TestUtils;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaIntRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmTestUtils.newIntRef;

public class ReadonlyRepeatableReadStressTest {

    private volatile boolean stop;
    private BetaIntRef ref;
    private int readThreadCount = 5;
    private int modifyThreadCount = 2;
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
            AtomicBlock block = stm.createTransactionFactoryBuilder()
                    .buildAtomicBlock();
            AtomicVoidClosure closure = new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    ref.getAndSet(btx, ref.get(btx));
                }
            };


            while (!stop) {
                block.execute(closure);
                sleepRandomMs(5);
            }
        }
    }

    class ReadThread extends TestThread {

        private final AtomicBlock readTrackingReadonlyBlock = stm.createTransactionFactoryBuilder()
                .setReadonly(true)
                .setReadTrackingEnabled(true)
                .buildAtomicBlock();

        private final AtomicBlock readTrackingUpdateBlock = stm.createTransactionFactoryBuilder()
                .setReadonly(false)
                .setReadTrackingEnabled(true)
                .buildAtomicBlock();

        private final AtomicVoidClosure closure = new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                BetaTransaction btx = (BetaTransaction) tx;

                int firstTime = ref.get(btx);
                sleepRandomMs(2);
                int secondTime = ref.get(btx);
                assertEquals(firstTime, secondTime);
            }
        };

        public ReadThread(int id) {
            super("ReadThread-" + id);
        }

        @Override
        public void doRun() {
            int k = 0;
            while (!stop) {
                switch (k % 2) {
                    case 0:
                        readTrackingReadonlyBlock.execute(closure);
                        break;
                    case 1:
                        readTrackingUpdateBlock.execute(closure);
                        break;
                    default:
                        throw new IllegalStateException();
                }
                k++;
            }
        }
    }
}
