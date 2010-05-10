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
public class WritePerformanceTest {

    private int readCount;
    private int threadCount;

    private ReadThread[] threads;
    private Ref ref;
    private boolean automaticReadTracking;


    @Before
    public void setUp() {
        clearThreadLocalTransaction();

        readCount = 100 * 1000 * 1000;
        threadCount = 1;

        threads = new ReadThread[threadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new ReadThread(k);
        }

        ref = new Ref();
    }

    @Test
    public void testWithReadTracking() {
        test(true);
    }

    @Test
    public void testWithoutReadTracking() {
        test(false);
    }

    public void test(boolean automaticReadTracking) {
        long startNs = System.nanoTime();

        startAll(threads);
        joinAll(threads);

        this.automaticReadTracking = automaticReadTracking;
        int transactionCount = threadCount * readCount;

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
                if ((k % 10000000) == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }

                write();
            }
        }

        public void write() {
            if(automaticReadTracking){
                ref.incWithReadTracking();
            }else{
                ref.incWithoutReadTracking();
            }
        }
    }

    @TransactionalObject
    static class Ref {
        private int value;

        public int get() {
            return value;
        }

        @TransactionalMethod(readonly = false, trackReads = false)
        public void incWithoutReadTracking() {
            value++;
        }

        @TransactionalMethod(readonly = false, trackReads = true)
        public void incWithReadTracking() {
            value++;
        }
    }

}
