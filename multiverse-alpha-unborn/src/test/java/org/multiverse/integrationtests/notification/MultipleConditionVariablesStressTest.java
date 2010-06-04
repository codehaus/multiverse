package org.multiverse.integrationtests.notification;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.TestUtils;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.Stm;
import org.multiverse.transactional.refs.IntRef;

import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * The goal of this test is to see if the system is able to deal with multiple objects
 * that are used for the abort and retry. There is an array of objects to listen to.
 * Each thread listens to all objects, and if one of those objects has a value equal to 1,
 * it will decrease the value, and increase a random other value so that another transaction
 * can wake up.
 *
 * @author Peter Veentjer
 */
public class MultipleConditionVariablesStressTest {

    private int objectCount = 100;
    private int threadCount = 10;
    private Stm stm;
    private IntRef[] values;
    private volatile boolean stop;

    @Before
    public void setUp() {
        stop = false;
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();

        values = new IntRef[objectCount];
        for (int k = 0; k < objectCount; k++) {
            values[k] = new IntRef(0);
        }
    }

    @Test
    public void test() {
        WorkerThread[] threads = createThreads();
        values[0].inc();

        startAll(threads);
        sleepMs(TestUtils.getStressTestDurationMs(60 * 1000));
        stop = true;
        joinAll(threads);
    }

    public WorkerThread[] createThreads() {
        WorkerThread[] threads = new WorkerThread[threadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new WorkerThread(k);
        }
        return threads;
    }

    class WorkerThread extends TestThread {

        public WorkerThread(int id) {
            super("WorkerThread-" + id);
        }

        @Override
        public void doRun() {
            int count = 0;
            while (!stop) {
                doit();
                if (count % (10 * 1000) == 0) {
                    System.out.printf("%s is at count %s\n", getName(), count);
                }
                count++;
            }
        }

        @TransactionalMethod(maxRetries = 10000)
        public void doit() {
            for (IntRef value : values) {
                if (value.get() == 1) {
                    value.dec();
                    int random = TestUtils.randomInt(values.length - 1);
                    values[random].inc();
                    return;
                }
            }

            retry();
        }
    }
}
