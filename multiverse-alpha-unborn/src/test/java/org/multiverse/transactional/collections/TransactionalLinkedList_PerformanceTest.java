package org.multiverse.transactional.collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.multiverse.TestUtils.format;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalLinkedList_PerformanceTest {
    private Stm stm;
    private int iterations = 10;
    private int count = 1000 * 1000;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void testRelaxedTransactionalLinkedList() {
        TransactionalLinkedList<Integer> list = new TransactionalLinkedList<Integer>(10 * 1000 * 1000, true);

        long startNs = System.nanoTime();

        for (int l = 0; l < iterations; l++) {
            for (int k = 0; k < count; k++) {
                list.add(k);
            }
            list.clear();
            System.out.println("completed iteration " + l);
        }

        long durationNs = System.nanoTime() - startNs;
        double transactionsPerSecond = (1.0d * count * iterations * TimeUnit.SECONDS.toNanos(1)) / durationNs;
        System.out.printf("Performance %s transactions/second\n", format(transactionsPerSecond));
    }

    @Test
    public void testStrictTransactionalLinkedList() {
        TransactionalLinkedList<Integer> list = new TransactionalLinkedList<Integer>(10 * 1000 * 1000, false);

        long startNs = System.nanoTime();

        for (int l = 0; l < iterations; l++) {
            for (int k = 0; k < count; k++) {
                list.add(k);
            }
            list.clear();
            System.out.println("completed iteration " + l);
        }

        long durationNs = System.nanoTime() - startNs;
        double transactionsPerSecond = (1.0d * count * iterations * TimeUnit.SECONDS.toNanos(1)) / durationNs;
        System.out.printf("Performance %s transactions/second\n", format(transactionsPerSecond));
    }

    @Test
    public void testLinkedBlockingQueue() {
        LinkedBlockingQueue<Integer> list = new LinkedBlockingQueue<Integer>();

        long startNs = System.nanoTime();

        for (int l = 0; l < iterations; l++) {
            for (int k = 0; k < count; k++) {
                list.add(k);
            }
            list.clear();
            System.out.println("completed iteration " + l);
        }

        long durationNs = System.nanoTime() - startNs;
        double transactionsPerSecond = (1.0d * count * iterations * TimeUnit.SECONDS.toNanos(1)) / durationNs;
        System.out.printf("Performance %s transactions/second\n", format(transactionsPerSecond));
    }
}

