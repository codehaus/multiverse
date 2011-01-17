package org.multiverse.stms.gamma.integration.traditionalsynchronization;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.LockMode;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicClosure;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.references.BooleanRef;
import org.multiverse.api.references.IntRef;
import org.multiverse.api.references.Ref;
import org.multiverse.stms.gamma.GammaStm;

import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class ConditionVariableStressTest {
    private GammaStm stm;
    private Stack stack;
    private int itemCount = 10000000;
    private LockMode lockMode;

    @Before
    public void setUp() {
        stm = (GammaStm) getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @Test
    public void testNoLocking() {
        test(LockMode.None);
    }

    @Test
    public void testReadLock() {
        test(LockMode.Read);
    }

    @Test
    public void testWriteLock() {
        test(LockMode.Write);
    }

    @Test
    public void testExclusiveLock() {
        test(LockMode.Exclusive);
    }

    public void test(LockMode lockMode) {
        this.lockMode = lockMode;
        stack = new Stack(100);

        PushThread pushThread = new PushThread();
        PopThread popThread = new PopThread();

        startAll(pushThread, popThread);
        joinAll(pushThread, popThread);
    }

    public class PushThread extends TestThread {

        public PushThread() {
            super("PushThread");
        }

        @Override
        public void doRun() throws Exception {
            for (int k = 0; k < itemCount; k++) {
                stack.push("foo");

                if (k % 100000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }
        }
    }

    public class PopThread extends TestThread {
        @Override
        public void doRun() throws Exception {
            for (int k = 0; k < itemCount; k++) {
                stack.pop();

                if (k % 100000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }
        }
    }

    class Stack {
        ConditionVariable isNotFull = new ConditionVariable(true);
        ConditionVariable isNotEmpty = new ConditionVariable(false);
        Ref<Node> head = newRef();
        IntRef size = newIntRef();
        final int capacity;
        final AtomicBlock pushBlock = stm.newTransactionFactoryBuilder()
                .setReadLockMode(lockMode)
                .buildAtomicBlock();
        final AtomicBlock popBlock = stm.newTransactionFactoryBuilder()
                .setReadLockMode(lockMode)
                .buildAtomicBlock();

        Stack(int capacity) {
            this.capacity = capacity;
        }

        void push(final String item) {
            pushBlock.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    isNotFull.awaitTrue();

                    head.set(new Node(item, head.get()));
                    size.increment();
                    if (size.get() == capacity) {
                        isNotFull.set(false);
                    }

                    isNotEmpty.set(true);
                }
            });
        }

        String pop() {
            return popBlock.execute(new AtomicClosure<String>() {
                @Override
                public String execute(Transaction tx) throws Exception {
                    isNotEmpty.awaitTrue();

                    Node node = head.get();
                    head.set(node.next);
                    size.decrement();
                    if (size.get() == 0) {
                        isNotEmpty.set(false);
                    }
                    isNotFull.set(true);
                    return node.value;
                }
            });
        }
    }

    class Node {
        final String value;
        final Node next;

        Node(String value, Node next) {
            this.value = value;
            this.next = next;
        }
    }

    class ConditionVariable {
        final BooleanRef ref;

        ConditionVariable(boolean value) {
            this.ref = newBooleanRef(value);
        }

        void awaitTrue() {
            ref.await(true);
        }

        void awaitFalse() {
            ref.await(false);
        }

        void set(boolean value) {
            ref.set(value);
        }
    }
}
