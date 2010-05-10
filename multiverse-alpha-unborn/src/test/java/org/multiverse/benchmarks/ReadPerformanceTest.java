package org.multiverse.benchmarks;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;

import java.util.concurrent.TimeUnit;

import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class ReadPerformanceTest {

    private long readCount;
    private boolean readonly;

    private ReadThread[] threads;
    private Ref ref;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();

        readCount = 100 * 1000 * 1000;

        readonly = true;

        ref = new Ref();
    }

    @Test
    public void test_1() {
        test(1);
    }

    @Test
    public void test_2() {
        test(2);
    }

    @Test
    public void test_4() {
        test(4);
    }

    @Test
    public void test_8() {
        test(8);
    }

    @Test
    public void test_16() {
        test(16);
    }

    public void test(int threadCount) {
        threads = new ReadThread[threadCount];

        for (int k = 0; k < threads.length; k++) {
            threads[k] = new ReadThread(k);
        }

        long startNs = System.nanoTime();

        startAll(threads);
        joinAll(threads);

        long transactionCount = threadCount * readCount;

        long durationNs = System.nanoTime() - startNs;
        double transactionsPerSecond = (1.0d * transactionCount * TimeUnit.SECONDS.toNanos(1)) / durationNs;
        System.out.printf("Performance %s transactions/second\n", format(transactionsPerSecond));
    }

    public class ReadThread extends TestThread {

        public ReadThread(int id) {
            super("ReadThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            for (int k = 0; k < readCount; k++) {
                if ((k % 1000000000) == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }

                if (readonly) {
                    readInReadonlyMode();
                } else {
                    readInUpdateMode();
                }
            }
        }

        @TransactionalMethod(readonly = true)
        public int readInReadonlyMode() {
            return ref.get();
        }

        @TransactionalMethod(readonly = false)
        public int readInUpdateMode() {
            return ref.get();
        }
    }

    @TransactionalObject
    static class Ref {
        private int value;

        public int get() {
            return value;
        }

        public void set(int newValue) {
            this.value = value;
        }
    }
}
