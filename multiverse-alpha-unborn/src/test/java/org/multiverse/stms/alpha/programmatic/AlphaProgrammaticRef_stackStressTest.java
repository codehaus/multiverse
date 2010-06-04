package org.multiverse.stms.alpha.programmatic;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.api.programmatic.ProgrammaticRef;
import org.multiverse.api.programmatic.ProgrammaticRefFactory;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticRef_stackStressTest {

    private int threadCount = 10;
    private volatile boolean stop;
    private int stackCapacity = 1000;

    private final static ProgrammaticRefFactory refFactory = getGlobalStmInstance()
            .getProgrammaticRefFactoryBuilder()
            .build();

    private Stack<String> stack;
    private final static String POISON = "poison";

    @Before
    public void setUp() {
        stop = false;
        clearThreadLocalTransaction();
        stack = new Stack();
    }

    @Before
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void test() {
        ProducerThread[] producers = new ProducerThread[threadCount];
        ConsumerThread[] consumers = new ConsumerThread[threadCount];
        for (int k = 0; k < threadCount; k++) {
            producers[k] = new ProducerThread(k);
            consumers[k] = new ConsumerThread(k);
        }

        startAll(producers);
        startAll(consumers);
        sleepMs(getStressTestDurationMs(20 * 1000));
        stop = true;
        joinAll(producers);
        joinAll(consumers);

        assertEquals(sum(producers), sum(consumers));
    }

    private long sum(ProducerThread[] threads){
        long sum = 0;
        for(ProducerThread t: threads){
            sum+=t.produceCount;
        }
        return sum;
    }

    private long sum(ConsumerThread[] threads){
        long sum = 0;
        for(ConsumerThread t: threads){
            sum+=t.consumeCount;
        }
        return sum;
    }

    public class ProducerThread extends TestThread {
        private long produceCount;

        public ProducerThread(int id) {
            super("ProducerThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            while (!stop) {
                produce();

                if (produceCount % 100000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), produceCount);
                }

                produceCount++;
            }

            stack.push(POISON);
        }

        @TransactionalMethod
        private void produce() {
            stack.push("foo");
        }
    }

    public class ConsumerThread extends TestThread {
        private long consumeCount;

        public ConsumerThread(int id) {
            super("ConsumerThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            boolean again;
            do {
                again = consume();
                if(again){
                    consumeCount++;
                }

                if (consumeCount % 100000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), consumeCount);
                }
            } while (again);
        }

        @TransactionalMethod
        private boolean consume() {
            String item = stack.pop();
            return !POISON.equals(item);
        }
    }

    @TransactionalObject
    public class Stack<E> {

        private final ProgrammaticRef<Node<E>> head = refFactory.atomicCreateRef(null);

        int size() {
            Node<E> h = head.get();
            return h == null ? 0 : h.size;
        }

        void push(E item) {
            if (item == null) {
                throw new NullPointerException();
            }

            if (size() == stackCapacity) {
                retry();
            }

            head.set(new Node<E>(item, head.get()));
        }

        E pop() {
            if (head.isNull()) {
                retry();
            }

            Node<E> h = head.get();
            head.set(h.next);
            return h.value;
        }

        class Node<E> {
            final E value;
            final Node<E> next;
            final int size;

            Node(E value, Node<E> next) {
                this.value = value;
                this.next = next;
                this.size = next == null ? 1 : next.size + 1;
            }
        }
    }
}
