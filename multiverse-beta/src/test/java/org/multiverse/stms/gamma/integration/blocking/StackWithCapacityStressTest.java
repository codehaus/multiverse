package org.multiverse.stms.gamma.integration.blocking;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.LockMode;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicClosure;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaIntRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaRef;

import java.util.HashSet;
import java.util.LinkedList;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * The test is not very efficient since a lot of temporary objects like the transaction template are created.
 * But that is alright for this test since it isn't a benchmark.
 *
 * @author Peter Veentjer.
 */
public class StackWithCapacityStressTest implements GammaConstants {

    private GammaStm stm;
    private int itemCount = 2 * 1000 * 1000;
    private Stack<Integer> stack;
    private int maxCapacity = 1000;
    private LockMode lockMode;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = (GammaStm) getGlobalStmInstance();
    }

    @Test
    public void testWithNoLocks() {
        test(LockMode.None);
    }

    @Test
    public void testWithReadLock() {
        test(LockMode.Read);
    }

    @Test
    public void testWithWriteLock() {
        test(LockMode.Write);
    }

    @Test
    public void testWithCommitLock() {
        test(LockMode.Exclusive);
    }


    public void test(LockMode lockMode) {
        this.lockMode = lockMode;

        stack = new Stack<Integer>();

        ProduceThread produceThread = new ProduceThread();
        ConsumeThread consumeThread = new ConsumeThread();

        startAll(produceThread, consumeThread);
        joinAll(produceThread, consumeThread);

        System.out.println("finished executing");

        assertEquals(itemCount, produceThread.producedItems.size());
        assertEquals(
                new HashSet<Integer>(produceThread.producedItems),
                new HashSet<Integer>(consumeThread.consumedItems));
    }

    class ConsumeThread extends TestThread {

        private final LinkedList<Integer> consumedItems = new LinkedList<Integer>();

        public ConsumeThread() {
            super("ConsumeThread");
        }

        @Override
        public void doRun() throws Exception {
            for (int k = 0; k < itemCount; k++) {
                int item = stack.pop();
                consumedItems.add(item);

                if (k % 100000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }
        }
    }

    class ProduceThread extends TestThread {

        private final LinkedList<Integer> producedItems = new LinkedList<Integer>();

        public ProduceThread() {
            super("ProduceThread");
        }

        @Override
        public void doRun() throws Exception {
            for (int k = 0; k < itemCount; k++) {
                stack.push(k);
                producedItems.add(k);

                if (k % 100000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }
        }
    }

    class Stack<E> {
        private final GammaRef<Node<E>> head = new GammaRef<Node<E>>(stm);
        private final GammaIntRef size = new GammaIntRef(stm);
        private final AtomicBlock pushBlock = stm.createTransactionFactoryBuilder()
                .setReadLockMode(lockMode)
                .buildAtomicBlock();
        private final AtomicBlock popBlock = stm.createTransactionFactoryBuilder()
                .setReadLockMode(lockMode)
                .buildAtomicBlock();

        public void push(final E item) {
            pushBlock.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    if (size.get() >= maxCapacity) {
                        retry();
                    }

                    size.increment();
                    head.set(new Node<E>(item, head.get()));
                }
            });
        }

        public E pop() {
            return popBlock.execute(new AtomicClosure<E>() {
                @Override
                public E execute(Transaction tx) throws Exception {
                    if (head.isNull()) {
                        retry();
                    }

                    size.decrement();
                    Node<E> node = head.get();
                    head.set(node.next);
                    return node.item;
                }
            });
        }
    }

    class Node<E> {
        final E item;
        Node<E> next;

        Node(E item, Node<E> next) {
            this.item = item;
            this.next = next;
        }
    }

}
