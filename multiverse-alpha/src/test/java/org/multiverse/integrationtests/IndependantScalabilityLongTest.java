package org.multiverse.integrationtests;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import org.multiverse.api.Stm;
import org.multiverse.transactional.primitives.TransactionalInteger;

import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class IndependantScalabilityLongTest {
    private Stm stm;
    private long updateCount = 1000000;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void test() {
        for (int k = 1; k <= Runtime.getRuntime().availableProcessors(); k++) {
            test(k);
        }
    }

    public void test(int threadCount) {
        System.out.printf("starting with %s threads\n", threadCount);

        long startMs = System.currentTimeMillis();
        TestThread[] threads = createThreads(threadCount);
        startAll(threads);
        joinAll(threads);
        long endMs = System.currentTimeMillis();
        System.out.printf("execution took %s ms\n", (endMs - startMs));
    }

    public MyThread[] createThreads(int threadCount) {
        MyThread[] threads = new MyThread[threadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new MyThread(k, new TransactionalInteger(0));
        }
        return threads;
    }

    class MyThread extends TestThread {
        private TransactionalInteger ref;

        public MyThread(int id, TransactionalInteger ref) {
            super("Thread-" + id);
            this.ref = ref;
        }

        @Override
        public void doRun() {
            for (int k = 0; k < updateCount; k++) {
                ref.inc();
            }
        }
    }
}
