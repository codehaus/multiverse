package org.multiverse.integrationtests.isolation;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.TestUtils;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.ThreadLocalTransaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.transactional.refs.IntRef;

import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;


/**
 * This test that checks that the no dirty reads (so writes made by transactions in progress) are seen.
 * <p/>
 * The Test: a shared integer value that is increased by a modification thread. When the transaction begins,
 * it is increased (so the value can't be divided by 2) there will be a delay, another increase (so that
 * the value can be divided by 2) and the transaction commits. Another observing thread that looks at this
 * value should never see a value that can't be divided by 2.
 * <p/>
 *
 * @author Peter Veentjer.
 */
public class DirtyReadStressTest {

    private IntRef ref;

    private int readThreadCount = 10;
    private int modifyThreadCount = 2;

    private volatile boolean stop;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        ref = new IntRef(0);
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
        sleepMs(TestUtils.getStressTestDurationMs(60 * 1000));
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

        @TransactionalMethod
        private void modify() {
            ref.inc();
            //make sure that the dirty value is 'out there' some time.
            sleepRandomMs(30);
            ThreadLocalTransaction.getThreadLocalTransaction().abort();
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
                switch (k% 4){
                    case 0:
                        observeUsingReadonlyTransaction();
                        break;
                    case 1:
                        observeUsingReadtrackingReadonlyTransaction();
                        break;
                    case 2:
                        observeUsingUpdateTransaction();
                        break;
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

        @TransactionalMethod(readonly = true, trackReads = false)
        private void observeUsingReadonlyTransaction() {
            observe();
        }


        @TransactionalMethod(readonly = false, trackReads = false)
        private void observeUsingUpdateTransaction() {
            observe();
        }

        @TransactionalMethod(readonly = true, trackReads = true)
        private void observeUsingReadtrackingReadonlyTransaction() {
            observe();
        }


        @TransactionalMethod(readonly = false, trackReads = true)
        private void observeUsingReadtrackingUpdateTransaction() {
            observe();
        }

        private void observe() {
            if (ref.get() % 2 != 0) {
                fail();
            }
        }
    }
}
