package org.multiverse.transactional.arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalReferenceArray_readwriteStressTest {

    private int refCount = 1000;
    private int threadCount = 10;
    private TransactionalReferenceArray<Integer> array;
    private int transactionCount = 1000 * 1000;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void test() {
        array = new TransactionalReferenceArray<Integer>(refCount);
        for (int k = 0; k < array.length(); k++) {
            array.set(k, 0);
        }

        WorkerThread[] threads = new WorkerThread[threadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new WorkerThread(k);
        }

        startAll(threads);
        joinAll(threads);

        assertEquals(sum(threads), sum());
    }

    public int sum(WorkerThread[] threads) {
        int result = 0;
        for (WorkerThread thread : threads) {
            result += thread.incCount;
        }
        return result;
    }


    public int sum() {
        int result = 0;
        for (int k = 0; k < array.length(); k++) {
            result += array.get(k);
        }
        return result;
    }

    class WorkerThread extends TestThread {

        private int incCount;

        public WorkerThread(int id) {
            super("WorkerThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            for (int k = 0; k < transactionCount; k++) {
                incCount += doit();

                if (k % 100000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }
        }

        @TransactionalMethod
        private int doit() {
            int inc = 0;

            for (int k = 0; k < array.length(); k++) {
                if (randomOneOf(refCount / 10)) {
                    array.set(k, array.get(k) + 1);
                    inc++;
                }
            }

            return inc;
        }
    }
}
