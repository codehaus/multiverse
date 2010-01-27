package org.multiverse.transactional.executors;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.System.nanoTime;

public class TransactionalThreadPoolExecutor_stressLongTest {

    private TransactionalThreadPoolExecutor executor;

    private static final int scheduleCount = 2 * 1000 * 1000;

    private AtomicInteger runningCount;

    @Before
    public void setUp() {
        executor = new TransactionalThreadPoolExecutor();
        runningCount = new AtomicInteger(executor.getCorePoolSize());
    }

    @Test
    public void test() {
        long startNs = nanoTime();

        for (int k = 0; k < executor.getCorePoolSize(); k++) {
            executor.execute(new Command(k));
        }
        executor.awaitTerminationUninterruptibly();

        long durationNs = nanoTime() - startNs;
        double performance = (1.0d * scheduleCount * executor.getCorePoolSize() * TimeUnit.SECONDS.toNanos(1)) / durationNs;

        System.out.printf("Duration: %s ms\n", TimeUnit.NANOSECONDS.toMillis(durationNs));
        System.out.printf("Tasks executed: %s\n", scheduleCount * executor.getCorePoolSize());
        System.out.printf("Performance: %s tasks/second\n", performance);
    }

    public class Command implements Runnable {
        private int count;
        private final int id;

        public Command(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            count++;

            if (count % 100000 == 0) {
                System.out.printf("Command %s is at %s\n", id, count);
            }

            if (count < scheduleCount) {
                executor.execute(this);
            } else {
                if (runningCount.decrementAndGet() == 0) {
                    executor.shutdown();
                }
            }
        }
    }
}
