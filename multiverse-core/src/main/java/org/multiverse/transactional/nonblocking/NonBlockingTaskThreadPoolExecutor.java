package org.multiverse.transactional.nonblocking;

import org.multiverse.annotations.TransactionalObject;
import org.multiverse.api.Transaction;
import org.multiverse.transactional.executors.TransactionalThreadPoolExecutor;

/**
 * Waarom zou je meer transacties willen? Ivm blocking van transaction. Atm heeft iedere transactie zijn eigen thread
 * nodig. Maar kan een thread ook meerdere transacties verwerken?
 * <p/>
 * Problems
 * <ol>
 * <li>
 * </li>
 * </ol>
 *
 * @author Peter Veentjer.
 */
@TransactionalObject
public class NonBlockingTaskThreadPoolExecutor implements NonBlockingTransactionExecutor {

    private Thread[] workerThreads;
    private int threadCount;
    private TransactionSelector selector;
    private TransactionalThreadPoolExecutor executor;
    private State state;

    public NonBlockingTaskThreadPoolExecutor(int threadCount) {
        this(new DefaultTransactionSelector<SelectionKey>(), threadCount);
    }

    public NonBlockingTaskThreadPoolExecutor(TransactionSelector selector, int threadCount) {
        if (selector == null) {
            throw new NullPointerException();
        }
        this.threadCount = threadCount;
        this.selector = selector;
        this.state = State.unstarted;
        this.executor = new TransactionalThreadPoolExecutor(threadCount);
    }

    public boolean isShutdown() {
        return executor.isShutdown();
    }

    public boolean isTerminated() {
        return executor.isTerminated();
    }

    public void start() {
        executor.start();
    }

    public void shutdown() {

    }

    @Override
    public void execute(final NonBlockingTask task) {
        if (task == null) {
            throw new NullPointerException();
        }

        selector.register(new Key(task));
    }

    class Key implements TransactionSelectionKey {

        private final NonBlockingTask task;

        Key(NonBlockingTask task) {
            this.task = task;
        }

        @Override
        public Transaction getTransaction() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }

    public class Worker implements Runnable {

        @Override
        public void run() {
            /*
            try {
                while (!isShutdown()) {
                    SelectionKey key = selector.select();
                    run(key.task);
                }
            } catch (InterruptedException ex) {
            } */
        }

        private void run(NonBlockingTask task) {
            /*
            Transaction t;
            if (task.previous == null) {
                t = task.transactionalTask.getTransactionFactory().start();
            } else {
                t = task.previous;
                t.restart();
            }

            setThreadLocalTransaction(t);
            try {
                boolean again = task.execute(t);
                t.commit();
                if (again) {
                    //todo: return value ignored here.. s
                    tasks.offer(new NonBlockingTaskWrapper(task.transactionalTask));
                }
            } catch (Retry ex) {
                Latch latch = new NonBlockingLatch(t, task.transactionalTask);
                t.registerRetryLatch(latch);
                ///System.out.println("Retry encountered");

            } */
        }
    }

    static class SelectionKey implements TransactionSelectionKey {

        private final NonBlockingTask task;

        SelectionKey(NonBlockingTask task) {
            this.task = task;
        }

        @Override
        public Transaction getTransaction() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }

    enum State {
        unstarted, started, shutdown, terminated
    }
}

