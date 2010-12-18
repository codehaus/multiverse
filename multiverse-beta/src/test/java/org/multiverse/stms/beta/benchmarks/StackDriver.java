package org.multiverse.stms.beta.benchmarks;

import org.benchy.BenchmarkDriver;
import org.benchy.TestCaseResult;
import org.multiverse.TestThread;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicClosure;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.references.IntRef;
import org.multiverse.api.references.Ref;
import org.multiverse.stms.beta.BetaStm;

import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.api.StmUtils.newIntRef;

public class StackDriver extends BenchmarkDriver {

    private int pushThreadCount = 1;
    private int popThreadCount = 1;
    private int capacity = Integer.MAX_VALUE;
    private boolean poolClosures = false;
    private PessimisticLockLevel lockMode = PessimisticLockLevel.LockNone;
    private boolean dirtyCheck = false;

    private BetaStm stm;
    private PopThread[] popThreads;
    private PushThread[] pushThreads;
    private Stack<String> stack;

    @Override
    public void setUp() {
        System.out.printf("Multiverse > Pop threadcount %s\n", pushThreadCount);
        System.out.printf("Multiverse > Push threadcount %s\n", popThreadCount);
        System.out.printf("Multiverse > Capacity %s\n", capacity);
        System.out.printf("Multiverse > Pool Closures %s\n", poolClosures);
        System.out.printf("Multiverse > PessimisticLockLevel %s\n", lockMode);
        System.out.printf("Multiverse > DirtyCheck %s\n", dirtyCheck);

        stm = new BetaStm();
        stack = new Stack<String>();

        pushThreads = new PushThread[pushThreadCount];
        for (int k = 0; k < pushThreadCount; k++) {
            pushThreads[k] = new PushThread(k, stack);
        }

        popThreads = new PopThread[popThreadCount];
        for (int k = 0; k < popThreadCount; k++) {
            popThreads[k] = new PopThread(k, stack);
        }
    }

    @Override
    public void run(TestCaseResult testCaseResult) {
        startAll(pushThreads);
        startAll(popThreads);

        joinAll(pushThreads);
        joinAll(popThreads);
    }

    @Override
    public void processResults(TestCaseResult testCaseResult) {
        long pushCount = 0;
        long totalDuration = 0;
        for (PushThread t : pushThreads) {
            pushCount += t.count;
            totalDuration += t.getDurationMs();
        }

        long popCount = 0;
        for (PopThread t : popThreads) {
            popCount += t.count;
            totalDuration += t.getDurationMs();
        }

        long count = pushCount + popCount;
        System.out.printf("Multiverse > Total number of transactions %s\n", count);
    }

    class PushThread extends TestThread {
        private final Stack<String> stack;
        private long count;
        private final AtomicBlock pushBlock = stm.createTransactionFactoryBuilder()
                .setDirtyCheckEnabled(dirtyCheck)
                .setPessimisticLockLevel(lockMode)
                .buildAtomicBlock();

        public PushThread(int id, Stack<String> stack) {
            super("PushThread-" + id);
            this.stack = stack;
        }

        @Override
        public void doRun() throws Exception {
            if (poolClosures) {
                runWithPooledClosure();
            } else {
                runWithoutPooledClosure();
            }
        }

        private void runWithoutPooledClosure() {
            while (!shutdown) {
                pushBlock.execute(new AtomicVoidClosure() {
                    @Override
                    public void execute(Transaction tx) throws Exception {
                        stack.push(tx, "item");
                    }
                });
                count++;
            }

            pushBlock.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    stack.push(tx, "end");
                }
            });
        }

        private void runWithPooledClosure() {
            final PushClosure pushClosure = new PushClosure();

            while (!shutdown) {
                pushClosure.item = "item";
                pushBlock.execute(pushClosure);
                count++;
            }

            pushClosure.item = "end";
            pushBlock.execute(pushClosure);
        }

        class PushClosure implements AtomicVoidClosure {
            String item;

            @Override
            public void execute(Transaction tx) throws Exception {
                stack.push(tx, item);
            }
        }
    }

    class PopThread extends TestThread {

        private final Stack<String> stack;
        private long count;
        private final AtomicBlock popBlock = stm.createTransactionFactoryBuilder()
                .setDirtyCheckEnabled(dirtyCheck)
                .setPessimisticLockLevel(lockMode)
                .buildAtomicBlock();

        public PopThread(int id, Stack<String> stack) {
            super("PopThread-" + id);
            this.stack = stack;
        }

        @Override
        public void doRun() throws Exception {
            if (poolClosures) {
                runWithoutPooledClosure();
            } else {
                runWithPooledClosure();
            }
        }

        private void runWithPooledClosure() {
            while (!shutdown) {
                popBlock.execute(new AtomicVoidClosure() {
                    @Override
                    public void execute(Transaction tx) throws Exception {
                        stack.pop(tx);
                    }
                });
                count++;
            }
        }

        private void runWithoutPooledClosure() {
            PopClosure popClosure = new PopClosure();
            while (!shutdown) {
                popBlock.execute(popClosure);
                count++;
            }
        }

        class PopClosure implements AtomicClosure<String> {
            @Override
            public String execute(Transaction tx) throws Exception {
                return stack.pop(tx);
            }
        }
    }

    class Stack<E> {
        private final Ref<StackNode<E>> head = stm.getDefaultRefFactory().newRef(null);
        private final IntRef size = newIntRef();

        public void push(Transaction tx, final E item) {
            if (capacity != Integer.MAX_VALUE) {
                if (size.get(tx) == capacity) {
                    tx.retry();
                }
                size.increment(tx);
            }
            head.set(tx, new StackNode<E>(item, head.get(tx)));
        }

        public E pop(Transaction tx) {
            E value = head.awaitNotNullAndGet(tx).value;

            if (capacity != Integer.MAX_VALUE) {
                size.decrement(tx);
            }
            return value;
        }
    }

    static class StackNode<E> {
        final E value;
        final StackNode<E> next;

        StackNode(E value, StackNode<E> next) {
            this.value = value;
            this.next = next;
        }
    }
}
