package org.multiverse.integrationtests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import static org.multiverse.TestUtils.*;
import org.multiverse.api.annotations.AtomicMethod;
import org.multiverse.datastructures.refs.IntRef;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * A Tests that makes sure that normaly Transactions are not the subject to deadlocks.
 * Normally resources are locked and this could lead to a deadlock. With TL2Stm resources
 * are locked only for a small amount of time, if the lock can't be acquired, all locks
 * are released.
 *
 * @author Peter Veentjer.
 */
public class NormalTransactionWontDeadlockLongTest {

    private int threadCount = 100;
    private int resourceCount = 10;
    private int transactionCount = 200;
    private IntRef[] values;

    @Before
    public void setUp() {
        setThreadLocalTransaction(null);

        values = new IntRef[resourceCount];
        for (int k = 0; k < values.length; k++) {
            values[k] = new IntRef();
        }
    }

    @After
    public void tearDown() {
        //    stm.getProfiler().print();
    }

    @Test
    public void test() {
        ModifyThread[] threads = createThreads();
        startAll(threads);
        joinAll(threads);
    }

    public ModifyThread[] createThreads() {
        ModifyThread[] threads = new ModifyThread[threadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new ModifyThread(k);
        }
        return threads;
    }

    public class ModifyThread extends TestThread {

        public ModifyThread(int id) {
            super("ModifyThread-" + id);
        }

        @Test
        public void doRun() {
            for (int k = 0; k < transactionCount; k++) {
                doit();
                if (k % 25 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }
        }

        @AtomicMethod
        private void doit() {
            IntRef value1 = values[randomInt(values.length - 1)];
            value1.inc();
            sleepMs(25);
            IntRef value2 = values[randomInt(values.length - 1)];
            value2.inc();
        }
    }
}