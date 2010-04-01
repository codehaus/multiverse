package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

/**
 * @author Peter Veentjer
 */
public class TransactionalLinkedList_PerformanceTest {
    private Stm stm;
    private int iterations = 50;
    private int count = 1000 * 1000;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
    }

    @Test
    public void testTransactionalLinkedList() {
        TransactionalLinkedList<Integer> list = new TransactionalLinkedList<Integer>();

        long startNs = System.nanoTime();

        for (int l = 0; l < iterations; l++) {
            for (int k = 0; k < count; k++) {
                list.add(k);
            }
            list.clear();
            System.out.println("completed run " + l);
        }

        long durationNs = System.nanoTime() - startNs;
        double transactionsPerSecond = (1.0d * count * iterations * TimeUnit.SECONDS.toNanos(1)) / durationNs;
        System.out.printf("Performance %s transactions/second\n", transactionsPerSecond);
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
            System.out.println("completed run " + l);
        }

        long durationNs = System.nanoTime() - startNs;
        double transactionsPerSecond = (1.0d * count * iterations * TimeUnit.SECONDS.toNanos(1)) / durationNs;
        System.out.printf("Performance %s transactions/second\n", transactionsPerSecond);
    }
}

