package org.multiverse.stms.alpha.programmatic;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.api.programmatic.ProgrammaticLongRef;
import org.multiverse.api.programmatic.ProgrammaticRef;
import org.multiverse.api.programmatic.ProgrammaticRefFactory;

import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * An integration tests that builds a queue (based on 2 stacks) based on the programmatic
 * reference api. It uses an AlphaProgrammaticLongRef and AlphaProgrammaticRef.
 *
 * @author Peter Veentjer
 */
public class AlphaProgrammaticLongRefAndRef_integrationStressTest {

    private int threadCount = 10;
    private int transactionCount = 1000 * 1000;
    private int queueCapacity = 1000;

    private final static ProgrammaticRefFactory refFactory = getGlobalStmInstance()
            .getProgrammaticRefFactoryBuilder()
            .build();

    private Queue<String> queue;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        queue = new Queue<String>();
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

        @TransactionalMethod(trackReads = true)
        private void produce() {
            queue.push("foo");
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

        @TransactionalMethod(trackReads = true)
        private void produce() {
            queue.pop();
        }
    }

    @TransactionalObject
    public class Queue<E> {
        private final Stack<E> queuedStack = new Stack<E>();
        private final Stack<E> popableStack = new Stack<E>();
        private final ProgrammaticLongRef size = refFactory.atomicCreateLongRef(0);

        public void push(E item) {
            if (size() > queueCapacity) {
                retry();
            }

            size.inc(1);
            queuedStack.push(item);
        }

        public E pop() {
            if (queuedStack.head.isNull() && popableStack.head.isNull()) {
                retry();
            }

            size.inc(-1);
            if (!popableStack.isEmpty()) {
                return popableStack.pop();
            }

            while (!queuedStack.isEmpty()) {
                popableStack.push(queuedStack.pop());
            }

            return popableStack.pop();
        }

        public int size() {
            return (int) size.get();
        }
    }

    @TransactionalObject
    public class Stack<E> {

        final ProgrammaticRef<Node<E>> head = refFactory.atomicCreateRef(null);

        public boolean isEmpty() {
            return head.isNull();
        }

        void push(E item) {
            if (item == null) {
                throw new NullPointerException();
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

            Node(E value, Node<E> next) {
                this.value = value;
                this.next = next;
            }
        }
    }
}
