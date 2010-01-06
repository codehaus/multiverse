package org.multiverse.integrationtests.readisolation;

import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import static org.multiverse.TestUtils.*;
import org.multiverse.api.annotations.AtomicMethod;
import org.multiverse.datastructures.refs.IntRef;
import org.multiverse.templates.AbortedException;
import org.multiverse.api.ThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;


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
public class DirtyReadLongTest {

    private IntRef intRef;

    private int readCount = 5000;
    private int readThreadCount = 10;
    private int modifyThreadCount = 2;

    private volatile boolean readersFinished = false;

    @Before
    public void setUp() {
        setThreadLocalTransaction(null);
        intRef = new IntRef(0);
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
                } catch (AbortedException ignore) {
                }

                sleepRandomMs(10);
            }
        }

        @AtomicMethod
        private void modify() {
            intRef.inc();
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

        @AtomicMethod(readonly = true)
        private void observeUsingReadonlyTransaction() {
            observe();
        }


        @AtomicMethod
        private void observeUsingUpdateTransaction() {
            observe();
        }

        private void observe() {
            if (intRef.get() % 2 != 0) {
                fail();
            }
        }
    }
}
