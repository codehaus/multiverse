package org.multiverse.stms.beta.benchmarks;

import org.benchy.BenchmarkDriver;
import org.benchy.TestCaseResult;
import org.multiverse.TestThread;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicClosure;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.references.Ref;
import org.multiverse.stms.beta.BetaStm;

import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;

public class BasicStackDriver extends BenchmarkDriver {

    private int pushThreadCount = 1;
    private int popThreadCount = 1;
    private BetaStm stm;
    private PopThread[] popThreads;
    private PushThread[] pushThreads;

    @Override
    public void setUp() {
        System.out.printf("Multiverse > Pop threadcount %s\n", pushThreadCount);
        System.out.printf("Multiverse > Push threadcount %s\n", popThreadCount);

        stm = new BetaStm();

        pushThreads = new PushThread[pushThreadCount];
        for (int k = 0; k < pushThreadCount; k++) {
            pushThreads[k] = new PushThread(k);
        }

        popThreads = new PopThread[popThreadCount];
        for (int k = 0; k < popThreadCount; k++) {
            popThreads[k] = new PopThread(k);
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
        //To change body of implemented methods use File | Settings | File Templates.
    }

    class PushThread extends TestThread {
        public PushThread(int id) {
            super("PushThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    }

    class PopThread extends TestThread {
        public PopThread(int id) {
            super("PopThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    }

    class Stack<E> {
        private final Ref<StackNode<E>> head = stm.getDefaultRefFactory().newRef(null);
        private final AtomicBlock pushBlock = stm.createTransactionFactoryBuilder().buildAtomicBlock();
        private final AtomicBlock popBlock = stm.createTransactionFactoryBuilder().buildAtomicBlock();

        public void push(final E item) {
            pushBlock.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    head.set(tx, new StackNode<E>(item, head.get(tx)));
                }
            });
        }

        public E pop() {
            return popBlock.execute(new AtomicClosure<E>() {
                @Override
                public E execute(Transaction tx) throws Exception {
                    return head.awaitNotNullAndGet(tx).value;
                }
            });
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
