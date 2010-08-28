package org.multiverse.stms.beta.integrationtest.blocking;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicClosure;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaRef;
import org.multiverse.stms.beta.transactionalobjects.RefTranlocal;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import java.util.LinkedList;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.createRef;
import static org.multiverse.stms.beta.ThreadLocalBetaObjectPool.getThreadLocalBetaObjectPool;

public class QueueWithoutCapacityStressTest {

    private boolean pessimistic;

    private BetaStm stm;
    private Queue<Integer> queue;
    private int itemCount = 2 * 1000 * 1000;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = new BetaStm();
        queue = new Queue<Integer>();
    }

    @Test
    public void testPessimistic() {
        test(true);
    }

    @Test
    public void testOptimistic() {
        test(false);
    }

    public void test(boolean pessimistic) {
        this.pessimistic = pessimistic;

        ProduceThread produceThread = new ProduceThread();
        ConsumeThread consumeThread = new ConsumeThread();

        startAll(produceThread, consumeThread);
        joinAll(produceThread, consumeThread);

        assertEquals(itemCount, produceThread.producedItems.size());
        assertEquals(produceThread.producedItems, consumeThread.consumedItems);
    }

    class ConsumeThread extends TestThread {

        private final LinkedList<Integer> consumedItems = new LinkedList<Integer>();

        public ConsumeThread() {
            super("ConsumeThread");
        }

        @Override
        public void doRun() throws Exception {
            for (int k = 0; k < itemCount; k++) {
                int item = queue.pop();
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
                queue.push(k);
                producedItems.add(k);

                if (k % 100000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }
        }
    }

    class Queue<E> {
        final Stack<E> pushedStack = new Stack<E>();
        final Stack<E> readyToPopStack = new Stack<E>();
        final AtomicBlock pushBlock = stm.getTransactionFactoryBuilder().buildAtomicBlock();
        final AtomicBlock popBlock = stm.getTransactionFactoryBuilder().buildAtomicBlock();

        public void push(final E item) {
            pushBlock.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    BetaObjectPool pool = getThreadLocalBetaObjectPool();
                    pushedStack.push(btx, pool, item);
                }
            });
        }

        public E pop() {
            return popBlock.execute(new AtomicClosure<E>() {
                @Override
                public E execute(Transaction tx) throws Exception {
                    BetaObjectPool pool = getThreadLocalBetaObjectPool();
                    BetaTransaction btx = (BetaTransaction) tx;

                    if (!readyToPopStack.isEmpty(btx, pool)) {
                        return readyToPopStack.pop(btx, pool);
                    }

                    while (!pushedStack.isEmpty(btx, pool)) {
                        E item = pushedStack.pop(btx, pool);
                        readyToPopStack.push(btx, pool, item);
                    }

                    return readyToPopStack.pop(btx, pool);
                }
            });
        }
    }

    class Stack<E> {
        final BetaRef<Node<E>> head = createRef(stm);

        void push(BetaTransaction tx, BetaObjectPool pool, E item) {
            RefTranlocal<Node<E>> headTranlocal = tx.openForWrite(head, pessimistic, getThreadLocalBetaObjectPool());
            headTranlocal.value = new Node<E>(item, headTranlocal.value);
        }

        boolean isEmpty(BetaTransaction tx, BetaObjectPool pool) {
            RefTranlocal<Node<E>> headTranlocal = tx.openForRead(head, pessimistic, pool);
            return headTranlocal.value == null;
        }

        E pop(BetaTransaction tx, BetaObjectPool pool) {
            RefTranlocal<Node<E>> headTranlocal = tx.openForWrite(head, pessimistic, pool);
            Node<E> node = headTranlocal.value;

            if (node == null) {
                retry();
            }

            headTranlocal.value = node.next;
            return node.item;
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
