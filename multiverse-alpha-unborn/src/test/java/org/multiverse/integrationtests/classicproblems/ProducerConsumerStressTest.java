package org.multiverse.integrationtests.classicproblems;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.ThreadLocalTransaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.transactional.collections.TransactionalLinkedList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * The Pipeline test is an integration test. There is a pipeline of producer/handover...handove/consumer threads
 * where the producer threads produces an item and puts it on the first queue, a consumer thread consumes it from
 * the last queue, and handover threads that handover items from one queue to the next. So it is a complex producer
 * consumer scenario.
 * <p/>
 * This is also an example of the classic producer/consumer problem:
 * http://en.wikipedia.org/wiki/Producers-consumers_problem
 *
 * @author Peter Veentjer.
 */
public class ProducerConsumerStressTest {

    private TransactionalLinkedList[] queues;
    private int queueCount = 50;
    private int itemCount = 5000;
    private int delayMs = 2;
    private boolean runWithAborts;
    private int queueCapacity = 50;
    private int concurrentHandoverCount;
    private boolean relaxedMaximumCapacity;

    @Before
    public void setUp() {
        setThreadLocalTransaction(null);
    }

    @Test
    public void testWithAbortsAndRelaxedCapacity() {
        runTest(true, true, 1);
    }

    @Test
    public void testWithoutAbortsAndRelaxedCapacity() {
        runTest(true, false, 1);
    }

    @Test
    public void testConcurrentHandoverWithAbortsAndRelaxedCapacity() {
        runTest(true, true, 5);
    }

    @Test
    public void testConcurrentHandoverWithoutAbortsAndRelaxedCapacity() {
        runTest(true, false, 5);
    }

    @Test
    public void testWithAbortsWithStrictCapacity() {
        runTest(false, true, 1);
    }

    @Test
    public void testWithoutAbortsWithStrictCapacity() {
        runTest(false, false, 1);
    }

    @Test
    public void testConcurrentHandoverWithAbortsAndStrictCapacity() {
        runTest(false, true, 5);
    }

    @Test
    public void testConcurrentHandoverWithoutAbortsAndWithStrictCapacity() {
        runTest(false, false, 5);
    }


    public void runTest(boolean relaxedMaximumCapacity, boolean runWithAborts, int concurrentHandoverCount) {
        this.runWithAborts = runWithAborts;
        this.concurrentHandoverCount = concurrentHandoverCount;
        this.relaxedMaximumCapacity = relaxedMaximumCapacity;
        queues = createQueues();

        ProducerThread producerThread = new ProducerThread();
        ConsumerThread consumerThread = new ConsumerThread();
        HandoverThread[] handoverThreads = createHandoverThreads(concurrentHandoverCount);

        startAll(producerThread, consumerThread);
        startAll(handoverThreads);

        joinAll(producerThread);
        joinAll(consumerThread);
        joinAll(handoverThreads);

        assertQueuesAreEmpty();

        if (concurrentHandoverCount == 1) {
            //make sure that all items produced are also consumed, and that the order also is untouched.
            assertEquals(producerThread.producedList, consumerThread.consumedList);
        } else {
            //since the ordering isn't preserved with multiple handover threads, we can't do a list equals.
            assertEquals(new HashSet(producerThread.producedList), new HashSet(consumerThread.consumedList));
        }
    }

    public HandoverThread[] createHandoverThreads(int concurrentHandoverCount) {
        System.out.println("queueCount " + queueCount);
        System.out.println("concurrentHandoverthreads " + concurrentHandoverCount);
        System.out.println("items " + ((queueCount - 1) * concurrentHandoverCount));

        HandoverThread[] threads = new HandoverThread[(queueCount - 1) * concurrentHandoverCount];
        System.out.println("thread.local: " + threads.length);
        int index = 0;
        for (int k = 0; k < queueCount - 1; k++) {
            TransactionalLinkedList<Integer> from = queues[k];
            TransactionalLinkedList<Integer> to = queues[k + 1];
            AtomicInteger remainingCounter = new AtomicInteger(itemCount);
            AtomicInteger delay = new AtomicInteger(1);
            for (int l = 0; l < concurrentHandoverCount; l++) {
                threads[index] = new HandoverThread(index, from, to, remainingCounter, delay);
                index++;
            }
        }
        return threads;
    }

    public void assertQueuesAreEmpty() {
        for (TransactionalLinkedList queue : queues) {
            if (!queue.isEmpty()) {
                fail();
            }
        }
    }

    private TransactionalLinkedList<Integer>[] createQueues() {
        TransactionalLinkedList<Integer>[] result = new TransactionalLinkedList[queueCount];
        for (int k = 0; k < queueCount; k++) {
            result[k] = new TransactionalLinkedList(queueCapacity, relaxedMaximumCapacity);
        }
        return result;
    }

    private class ProducerThread extends TestThread {
        private final List<Integer> producedList = new ArrayList<Integer>(itemCount);

        public ProducerThread() {
            setName("ProducerThread");
        }

        public void doRun() throws InterruptedException {
            for (int k = 0; k < itemCount; k++) {
                if (k % 500 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }

                boolean abort = runWithAborts && k % 2 == 0;

                if (abort) {
                    try {
                        produceOneItem(k, true);
                        fail();
                    } catch (DeadTransactionException expected) {
                    }
                }

                produceOneItem(k, false);

                producedList.add(k);
                sleepRandomMs(delayMs);
            }
        }

        @TransactionalMethod
        public void produceOneItem(int item, boolean abort) throws InterruptedException {
            TransactionalLinkedList<Integer> queue = queues[0];
            queue.putFirst(item);
            if (abort) {
                ThreadLocalTransaction.getThreadLocalTransaction().abort();
            }
        }
    }

    private class ConsumerThread extends TestThread {
        private final List consumedList = new LinkedList();

        public ConsumerThread() {
            setName("ConsumeThread");
        }

        public void doRun() throws InterruptedException {
            for (int k = 0; k < itemCount; k++) {
                if (k % 500 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }

                boolean abort = runWithAborts && k % 2 == 0;

                if (abort) {
                    try {
                        consumeOneItem(true);
                        fail();
                    } catch (DeadTransactionException expected) {
                    }
                }

                int item = consumeOneItem(false);
                sleepRandomMs(delayMs);
                consumedList.add(item);
            }
        }

        @TransactionalMethod
        public int consumeOneItem(boolean abort) throws InterruptedException {
            TransactionalLinkedList<Integer> queue = queues[queues.length - 1];
            int r = queue.takeLast();
            if (abort) {
                ThreadLocalTransaction.getThreadLocalTransaction().abort();
            }
            return r;
        }
    }

    private class HandoverThread extends TestThread {
        private final TransactionalLinkedList<Integer> from;
        private final TransactionalLinkedList<Integer> to;
        private final AtomicInteger remainingCounter;
        private final AtomicInteger aliveCount;

        public HandoverThread(int id, TransactionalLinkedList<Integer> from,
                              TransactionalLinkedList<Integer> to, AtomicInteger remainingCounter, AtomicInteger aliveCount) {
            setName("HandoverThread-" + id);
            this.from = from;
            this.to = to;
            this.remainingCounter = remainingCounter;
            this.aliveCount = aliveCount;
        }

        public void doRun() throws InterruptedException {
            int k = 0;
            while (remainingCounter.getAndDecrement() > 0) {
                if (k % 1000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }

                boolean abort = runWithAborts && k % 2 == 0;

                if (abort) {
                    try {
                        moveOneItem(true);
                        fail();
                    } catch (DeadTransactionException expected) {
                    }
                }

                moveOneItem(false);
                k++;
            }

            aliveCount.decrementAndGet();
        }

        @TransactionalMethod
        public void moveOneItem(boolean abort) throws InterruptedException {
            int item = from.takeLast();
            sleepRandomMs(aliveCount.get() * delayMs);
            to.putFirst(item);

            if (abort) {
                ThreadLocalTransaction.getThreadLocalTransaction().abort();
            }
        }
    }
}
