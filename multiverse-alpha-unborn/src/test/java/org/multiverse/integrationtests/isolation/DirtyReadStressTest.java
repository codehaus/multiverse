package org.multiverse.integrationtests.isolation;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.ThreadLocalTransaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.transactional.primitives.TransactionalInteger;

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

    private TransactionalInteger ref;

    private int readCount = 5000;
    private int readThreadCount = 10;
    private int modifyThreadCount = 2;

    private volatile boolean readersFinished = false;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        ref = new TransactionalInteger(0);
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
        joinAll(modifyThreads);
        joinAll(readerThread);
    }


    class FailingModifyThread extends TestThread {
        public FailingModifyThread(int threadId) {
            super("FailingModifyThread-" + threadId);
        }

        @Override
        public void doRun() {
            int k = 0;
            while (!readersFinished) {
                if (k % 1000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
                k++;
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
            try {
                for (int k = 0; k < readCount; k++) {
                    if (k % 1000 == 0) {
                        System.out.printf("%s is at %s\n", getName(), k);
                    }

                    if (k % 2 == 0) {
                        observeUsingReadonlyTransaction();
                    } else {
                        observeUsingUpdateTransaction();
                    }

                    sleepRandomMs(5);
                }
            } finally {
                readersFinished = true;
            }
        }

        @TransactionalMethod(readonly = true)
        private void observeUsingReadonlyTransaction() {
            observe();
        }


        @TransactionalMethod
        private void observeUsingUpdateTransaction() {
            observe();
        }

        private void observe() {
            if (ref.get() % 2 != 0) {
                fail();
            }
        }
    }
}
