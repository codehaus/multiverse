package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.ThreadLocalTransaction;
import org.multiverse.api.exceptions.DeadTransactionException;

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
 * @author Peter Veentjer
 */
public class TransactionalLinkedList_ProducerConsumerStressTest {

    private TransactionalLinkedList<Integer>[] queues;
    private int queueCount = 50;
    private int itemCount = 50000;
    private int delayMs = 2;
    private boolean runWithAborts;
    private int queueCapacity = 5000;
    private int concurrentHandoverCount;

    @Before
    public void setUp() {
        setThreadLocalTransaction(null);
    }

    @Test
    public void testWithAborts() {
        runTest(true, 1);
    }

    @Test
    public void testWithoutAborts() {
        runTest(false, 1);
    }

    @Test
    public void testConcurrentHandoverWithAborts() {
        runTest(true, 2);
    }

    @Test
    public void testConcurrentHandoverWithoutAborts() {
        runTest(false, 2);
    }

    public void runTest(boolean runWithAborts, int concurrentHandoverCount) {
        this.runWithAborts = runWithAborts;
        this.concurrentHandoverCount = concurrentHandoverCount;
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
        HandoverThread[] threads = new HandoverThread[(queueCount - 1) * concurrentHandoverCount];
        int index = 0;
        for (int k = 0; k < queueCount - 1; k++) {
            TransactionalLinkedList from = queues[k];
            TransactionalLinkedList to = queues[k + 1];
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

    private TransactionalLinkedList[] createQueues() {
        TransactionalLinkedList[] result = new TransactionalLinkedList[queueCount];
        for (int k = 0; k < queueCount; k++) {
            result[k] = new TransactionalLinkedList(queueCapacity);
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
                if (k % 2000 == 0) {
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

        @TransactionalMethod(automaticReadTrackingEnabled = true)
        public void produceOneItem(int item, boolean abort) throws InterruptedException {
            TransactionalLinkedList queue = queues[0];
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
                if (k % 2000 == 0) {
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
                //sleepRandomMs(delayMs);
                consumedList.add(item);
            }
        }

        @TransactionalMethod(automaticReadTrackingEnabled = true)
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

        public HandoverThread(int id, TransactionalLinkedList from, TransactionalLinkedList to, AtomicInteger remainingCounter, AtomicInteger aliveCount) {
            setName("HandoverThread-" + id);
            this.from = from;
            this.to = to;
            this.remainingCounter = remainingCounter;
            this.aliveCount = aliveCount;
        }

        public void doRun() throws InterruptedException {
            int k = 0;
            while (remainingCounter.getAndDecrement() > 0) {
                if (k % 2000 == 0) {
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

        @TransactionalMethod(automaticReadTrackingEnabled = true)
        public void moveOneItem(boolean abort) throws InterruptedException {
            int item = from.takeLast();
            //sleepRandomMs(aliveCount.get() * delayMs);
            to.putFirst(item);

            if (abort) {
                ThreadLocalTransaction.getThreadLocalTransaction().abort();
            }
        }
    }

}
