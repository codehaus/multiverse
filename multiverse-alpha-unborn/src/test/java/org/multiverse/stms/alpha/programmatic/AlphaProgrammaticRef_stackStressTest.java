package org.multiverse.stms.alpha.programmatic;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.api.programmatic.ProgrammaticRef;
import org.multiverse.api.programmatic.ProgrammaticRefFactory;

import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticRef_stackStressTest {

    private int threadCount = 10;
    private int transactionCount = 1000 * 1000;
    private int stackCapacity = 1000;

    private final static ProgrammaticRefFactory refFactory = getGlobalStmInstance()
            .getProgrammaticRefFactoryBuilder()
            .build();

    private Stack stack;

    @Before
    public void setUp() {
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
        joinAll(producers);
        joinAll(consumers);
    }

    public class ProducerThread extends TestThread {
        public ProducerThread(int id) {
            super("ProducerThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            for (int k = 0; k < transactionCount; k++) {
                produce();

                if (k % 100000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }
        }

        @TransactionalMethod
        private void produce() {
            stack.push("foo");
        }
    }

    public class ConsumerThread extends TestThread {
        public ConsumerThread(int id) {
            super("ConsumerThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            for (int k = 0; k < transactionCount; k++) {
                produce();

                if (k % 100000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }
        }

        @TransactionalMethod
        private void produce() {
            stack.pop();
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
