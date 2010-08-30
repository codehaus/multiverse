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
import org.multiverse.stms.beta.transactionalobjects.BetaIntRef;
import org.multiverse.stms.beta.transactionalobjects.BetaRef;
import org.multiverse.stms.beta.transactionalobjects.IntRefTranlocal;
import org.multiverse.stms.beta.transactionalobjects.RefTranlocal;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import java.util.HashSet;
import java.util.LinkedList;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.createIntRef;
import static org.multiverse.stms.beta.BetaStmUtils.createRef;
import static org.multiverse.stms.beta.ThreadLocalBetaObjectPool.getThreadLocalBetaObjectPool;

/**
 * The test is not very efficient since a lot of temporary objects like the transaction template are created.
 * But that is alright for this test since it isn't a benchmark.
 *
 * @author Peter Veentjer.
 */
public class StackWithCapacityStressTest {

    private BetaStm stm;
    private int itemCount = 2 * 1000 * 1000;
    private Stack<Integer> stack;
    private int maxCapacity = 1000;
    private boolean pessimistic;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = new BetaStm();
        stack = new Stack<Integer>();
    }

    @Test
    public void testPessimistic() {
        test(false);
    }

    @Test
    public void testOptimistic() {
        test(true);
    }

    public void test(boolean pessimistic) {
        this.pessimistic = pessimistic;

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
        private final BetaRef<Node<E>> head = createRef(stm);
        private final BetaIntRef size = createIntRef(stm);
        private final AtomicBlock pushBlock = stm.createTransactionFactoryBuilder().buildAtomicBlock();
        private final AtomicBlock popBlock = stm.createTransactionFactoryBuilder().buildAtomicBlock();

        public void push(final E item) {
            pushBlock.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaObjectPool objectPool = getThreadLocalBetaObjectPool();
                    BetaTransaction btx = (BetaTransaction) tx;

                    IntRefTranlocal sizeTranlocal = btx.openForWrite(size, pessimistic, objectPool);

                    if (sizeTranlocal.value >= maxCapacity) {
                        retry();
                    }

                    sizeTranlocal.value++;
                    RefTranlocal<Node<E>> headTranlocal = btx.openForWrite(head, pessimistic, objectPool);
                    headTranlocal.value = new Node<E>(item, headTranlocal.value);
                }
            });
        }

        public E pop() {
            return popBlock.execute(new AtomicClosure<E>() {
                @Override
                public E execute(Transaction tx) throws Exception {
                    BetaObjectPool objectPool = getThreadLocalBetaObjectPool();
                    BetaTransaction btx = (BetaTransaction) tx;

                    RefTranlocal<Node<E>> headTranlocal = btx.openForWrite(head, pessimistic, objectPool);

                    if (headTranlocal.value == null) {
                        retry();
                    }

                    IntRefTranlocal sizeTranlocal = btx.openForWrite(size, pessimistic, objectPool);
                    sizeTranlocal.value--;

                    E value = headTranlocal.value.item;
                    headTranlocal.value = headTranlocal.value.next;
                    return value;
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
