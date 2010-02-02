package org.multiverse.transactional.nonblocking;

import org.multiverse.annotations.TransactionalObject;
import org.multiverse.api.Transaction;
import org.multiverse.transactional.executors.TransactionalExecutor;

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
    private TransactionalExecutor executor;
    private State state;

    public NonBlockingTaskThreadPoolExecutor(int threadCount) {
        this(new DefaultTransactionSelector<NonBlockingTaskImpl>(), threadCount);
    }

    public NonBlockingTaskThreadPoolExecutor(TransactionSelector selector, int threadCount) {
        if (selector == null) {
            throw new NullPointerException();
        }
        this.threadCount = threadCount;
        this.selector = selector;
        this.state = State.unstarted;
    }

    public boolean isShutdown(){
        return false;
    }

    public boolean isTerminated(){
        return false;    
    }

    public void start() {

    }

    public void stop(){

    }

    @Override
    public void execute(final NonBlockingTask task) {
        if (task == null) {
            throw new NullPointerException();
        }

        Runnable registerCommand = new Runnable(){
            @Override
            public void run() {
                selector.register(new Key(task));
            }
        };

        executor.execute(registerCommand);
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

    /*
    public class Worker implements Runnable {

        @Override
        public void run() {
            try {
                while (!shutdown) {
                    NonBlockingTaskWrapper task = tasks.take();
                    run(task);
                }
            } catch (InterruptedException ex) {
            }
        }

        private void run(NonBlockingTask task) {
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
            } catch (RetryError ex) {
                Latch latch = new NonBlockingLatch(t, task.transactionalTask);
                t.registerRetryLatch(latch);
                ///System.out.println("RetryError encountered");

            }
        }
    } */

    static class NonBlockingTaskImpl implements TransactionSelectionKey {

        private final NonBlockingTask task;

        NonBlockingTaskImpl(NonBlockingTask task) {
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

