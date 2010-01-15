package org.multiverse.transactional.executors;

import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionLifecycleEvent;
import org.multiverse.api.TransactionLifecycleListener;
import org.multiverse.transactional.collections.TransactionalLinkedList;
import org.multiverse.utils.TodoException;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

/**
 * A transactional {@link java.util.concurrent.Executor}.
 * <p/>
 * Known concurrent limitations <ol> <li>no concurrent takes and puts</li> <li>no concurrent takes (not really a big
 * issue since the takes only have a very short transactions)</li> <li>no concurrent puts</li> </ol> This will
 * eventually be solved when a better queue implementation is found.
 * <p/>
 * todo: <ol> <li> Shutdown not implemented</li> <li> Shutdown now not implemented</li> <li>awaitTerminated not
 * implemented</li> <li>exception handling</li> </ol>
 * <p/>
 * A task executes by a TransactionalThreadPoolExecutor will not automatically receives its own transaction.
 *
 * @author Peter Veentjer.
 */
@TransactionalObject
public class TransactionalThreadPoolExecutor extends AbstractExecutorService {

    private final BlockingQueue<Runnable> workQueue;
    private final TransactionalLinkedList<Thread> threads = new TransactionalLinkedList<Thread>();

    private State state;
    private int maxPoolSize;
    private int corePoolSize;
    private ThreadFactory threadFactory;


    /**
     * Creates a new TransactionalThreadPoolExecutor with a unbound workqueue and 1 as corePoolSize and maxPoolSize.
     * <p/>
     * If no limit is placed on the workQueue is could lead to a system not degrading gracefull because it runs out of
     * memory instead of rejecting tasks.
     */
    public TransactionalThreadPoolExecutor() {
        this(new TransactionalLinkedList<Runnable>(), 1, 1);
    }

    /**
     * Creates a new TransactionalThreadPoolExecutor with the given workQueue and 1 as corePoolSize and maxPoolSize.
     *
     * @param workQueue the BlockingQueue to store unprocessed work.
     * @throws NullPointerException if workQueue is null.
     */
    public TransactionalThreadPoolExecutor(BlockingQueue<Runnable> workQueue) {
        this(workQueue, 1, 1);
    }

    /**
     * Creates a new TransactionalThreadPoolExecutor.
     *
     * @param workQueue    the BlockingQueue to store unprecces work.
     * @param corePoolSize
     * @param maxPoolSize
     * @throws NullPointerException     if workQueue is null.
     * @throws IllegalArgumentException if corePoolSize is smaller than zero or if maxPoolSize smaller than
     *                                  corePoolSize.
     */
    public TransactionalThreadPoolExecutor(BlockingQueue<Runnable> workQueue, int corePoolSize, int maxPoolSize) {
        if (workQueue == null) {
            throw new NullPointerException();
        }
        if (corePoolSize < 0) {
            throw new IllegalArgumentException();
        }
        if (maxPoolSize < corePoolSize) {
            throw new IllegalArgumentException();
        }

        this.maxPoolSize = maxPoolSize;
        this.corePoolSize = corePoolSize;
        this.workQueue = workQueue;
        this.state = State.unstarted;
        this.threadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "worker");
            }
        };
    }

    /**
     * Returns the minimum number of threads in this TransactionalThreadPoolExecutor. The returned value will always be
     * equal or larger than zero.
     *
     * @return the minimum number of threads in this TransactionalThreadPoolExecutor.
     */
    @TransactionalMethod(readonly = true)
    public int getCorePoolSize() {
        return corePoolSize;
    }

    /**
     * Returns the maximum number of threads in this TransactionalThreadPoolExecutor. The returned value will always be
     * equal or larger than {@link #getCorePoolSize()}. If the pool of threads doesn't grow, the returned value will be
     * equal to {@link #getCorePoolSize()}.
     *
     * @return the maximum number of thread in this TransactionalThreadPoolExecutor .
     */
    @TransactionalMethod(readonly = true)
    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    /**
     * Sets the maximum poolsize.
     *
     * @param newMaxPoolSize the new maximum poolsize
     * @throws IllegalArgumentException if newMaxPoolSize is smaller than 0
     * @throws IllegalStateException    if this TransactionalThreadPoolExecutor is shutdown or terminated.
     */
    @TransactionalMethod(automaticReadTracking = false)
    public void setMaxPoolSize(int newMaxPoolSize) {
        switch (state) {
            case unstarted:
                //fall through
            case started:
                if (newMaxPoolSize < 0) {
                    throw new IllegalArgumentException();
                }

                if (corePoolSize > newMaxPoolSize) {
                    corePoolSize = newMaxPoolSize;
                }

                maxPoolSize = newMaxPoolSize;
                break;
            case shutdown:
                throw new IllegalStateException();
            case terminated:
                throw new IllegalStateException();
        }
    }

    /**
     * Sets the corePoolSize of this TransactionalThreadPoolExecutor. If the TransactionalThreadPoolExecutor is
     * unstarted, it isn't started. If the newCorePoolSize is larger than the maximumPoolSize, it automatically sets the
     * maxPoolSize to newCorePoolSize.
     *
     * @param newCorePoolSize the new core poolsize.
     * @throws IllegalArgumentException if newCorePoolSize is smaller than 0.
     * @throws IllegalStateException    if this TransactionalThreadPoolExecutor is shutdown or terminated.
     */
    @TransactionalMethod(automaticReadTracking = false)
    public void setCorePoolSize(int newCorePoolSize) {
        switch (state) {
            case unstarted:
                //fall through
            case started:
                if (newCorePoolSize < 0) {
                    throw new IllegalArgumentException();
                }

                if (maxPoolSize < newCorePoolSize) {
                    maxPoolSize = newCorePoolSize;
                }

                corePoolSize = newCorePoolSize;
                break;
            case shutdown:
                throw new IllegalStateException();
            case terminated:
                throw new IllegalStateException();
        }
    }

    /**
     * Returns the current number of threads in this TransactionalThreadPoolExecutor.
     *
     * @return the current number of threads.
     */
    @TransactionalMethod(readonly = true)
    public int getCurrentPoolSize() {
        return threads.size();
    }

    /**
     * Returns the BlockingQueue this TransactionalThreadPoolExecutor uses to store unprocessed work.
     *
     * @return the BlockingQueue used to store unprocessed work.
     */
    @TransactionalMethod(readonly = true)
    public BlockingQueue<Runnable> getWorkQueue() {
        return workQueue;
    }

    /**
     * Returns the State this TransactionalThreadPoolExecutor has.
     *
     * @return the State this TransactionalThreadPoolExecutor has.
     */
    @TransactionalMethod(readonly = true)
    public State getState() {
        return state;
    }

    /**
     * Sets the thread factory used to create new threads.
     *
     * @param threadFactory the new thread factory
     * @throws NullPointerException if threadFactory is null
     * @see #getThreadFactory
     */
    @TransactionalMethod(automaticReadTracking = false)
    public void setThreadFactory(ThreadFactory threadFactory) {
        if (threadFactory == null) {
            throw new NullPointerException();
        }
        this.threadFactory = threadFactory;
    }

    /**
     * Returns the thread factory used to create new threads.
     *
     * @return the current thread factory
     * @see #setThreadFactory
     */
    @TransactionalMethod(readonly = true)
    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    @Override
    public void shutdown() {
        switch (state) {
            case unstarted:
                state = State.terminated;
                break;
            case started:
                state = State.shutdown;
                break;
            case shutdown:
                //the shutdown already is in progress, so ignore
                break;
            case terminated:
                //ignore it.
                break;
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    @TransactionalMethod(automaticReadTracking = false)
    public List<Runnable> shutdownNow() {
        switch (state) {
            case unstarted:
                state = State.terminated;
                return Collections.EMPTY_LIST;
            case started:
                if (threads.isEmpty()) {
                    state = State.terminated;
                }
                throw new TodoException();
            case shutdown:
                throw new TodoException();
            case terminated:
                //ignore it.
                return Collections.EMPTY_LIST;
            default:
                throw new IllegalStateException();
        }
    }


    @Override
    @TransactionalMethod(readonly = true)
    public boolean isShutdown() {
        return state == State.shutdown || state == State.terminated;
    }

    @Override
    @TransactionalMethod(readonly = true)
    public boolean isTerminated() {
        return state == State.terminated;
    }

    @Override
    @TransactionalMethod(readonly = true, automaticReadTracking = true)
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        switch (state) {
            case unstarted:
                throw new TodoException();
            case started:
                throw new TodoException();
            case shutdown:
                throw new TodoException();
            case terminated:
                return true;
            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Awaits for this TransactionalThreadPoolExecutor to complete. This call can be interrupted.
     *
     * @throws InterruptedException if the Thread was interrupted.
     */
    @TransactionalMethod(readonly = true, automaticReadTracking = true, interruptible = true)
    public void awaitTermination() throws InterruptedException {
        if (state != State.terminated) {
            retry();
        }
    }

    /**
     * Awaits for this TransactionalThreadPoolExecutor to complete without the possibility of being interrupted.
     */
    @TransactionalMethod(readonly = true, automaticReadTracking = true)
    public void awaitTerminationUninterruptibly() {
        if (state != State.terminated) {
            retry();
        }
    }

    @Override
    public void execute(Runnable command) {
        if (command == null) {
            throw new NullPointerException();
        }

        switch (state) {
            case unstarted:
                start();
                //fall through
            case started:
                boolean workOffered = workQueue.offer(command);
                if (!workOffered) {
                    throw new RejectedExecutionException();
                }

                if (threads.size() < maxPoolSize) {
                    Thread thread = createAndRegisterWorker();
                    getThreadLocalTransaction().register(new StartAllListener(thread));
                }
                break;
            case shutdown:
                throw new RejectedExecutionException();
            case terminated:
                throw new RejectedExecutionException();
            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Starts this TransactionalThreadPoolExecutor. If the TransactionalThreadPoolExecutor already is started, the call
     * is ignored.
     *
     * @throws IllegalStateException if the this TransactionalThreadPoolExecutor already is shutdown or terminated.
     */
    public void start() {
        switch (state) {
            case unstarted:
                state = State.started;
                Thread[] newThreads = new Thread[corePoolSize];
                for (int k = 0; k < corePoolSize; k++) {
                    Thread thread = createAndRegisterWorker();
                    newThreads[k] = thread;
                }
                getThreadLocalTransaction().register(new StartAllListener(newThreads));
                break;
            case started:
                //ignore call
                break;
            case shutdown:
                throw new IllegalStateException();
            case terminated:
                throw new IllegalStateException();
            default:
                throw new IllegalStateException();
        }
    }

    private Thread createAndRegisterWorker() {
        Worker worker = new Worker();
        Thread thread = threadFactory.newThread(worker);
        worker.thread = thread;
        threads.add(thread);
        return thread;
    }

    private void signalTerminated() {
        state = State.terminated;
    }

    enum State {

        unstarted, started, shutdown, terminated
    }

    private class Worker implements Runnable {

        private Thread thread;

        @Override
        public void run() {
            //it is important that the task is not processed under the same transaction as the item is removed
            //from the queue because it will limit concurrency and could also lead to the same task being
            //picked up by multiple threads (and all but one of them will fail when they do a commit).
            boolean again = true;
            do {

                try {
                    Runnable command = takeWork();
                    if (command == null) {
                        again = false;
                    } else {
                        execute(command);
                    }
                } catch (InterruptedException ex) {
                    //ignore, continue the loop.
                }
            } while (again);

            again = true;
            do {
                Runnable command = workQueue.poll();
                if (command == null) {
                    again = false;
                } else {
                    execute(command);
                }
            } while (again);

            threads.remove(thread);
            if (threads.isEmpty()) {
                signalTerminated();
            }
        }

        @TransactionalMethod
        private Runnable takeWork() throws InterruptedException {
            if (getState() == State.started) {
                return workQueue.take();
            } else {
                return null;
            }
        }

        private void execute(Runnable command) {
            try {
                command.run();
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                //todo
            }
        }
    }

    private static class StartAllListener implements TransactionLifecycleListener {

        private final Thread[] threads;

        private StartAllListener(Thread... threads) {
            this.threads = threads;
        }

        @Override
        public void notify(Transaction t, TransactionLifecycleEvent event) {
            if (event == TransactionLifecycleEvent.postCommit) {
                for (int k = 0; k < threads.length; k++) {
                    threads[k].start();
                }
            }
        }
    }

    private class InterruptAllListener implements TransactionLifecycleListener {

        @Override
        @TransactionalMethod
        public void notify(Transaction tx, TransactionLifecycleEvent event) {
            if (event == TransactionLifecycleEvent.postCommit) {
                for (Thread thread : threads) {
                    thread.interrupt();
                }
            }
        }
    }
}
