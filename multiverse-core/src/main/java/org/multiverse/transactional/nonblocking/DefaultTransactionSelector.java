package org.multiverse.transactional.nonblocking;

import org.multiverse.api.Transaction;
import org.multiverse.api.latches.Latch;
import org.multiverse.utils.TodoException;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The default {@link TransactionSelector} implementation.
 *
 * @author Peter Veentjer.
 * @param <K>
 */
public class DefaultTransactionSelector<K extends TransactionSelectionKey>
        implements TransactionSelector<K> {

    private final BlockingQueue<K> arrivedTasks;

    /**
     * Creates a new DefaultTransactionSelector with a LinkedBlockingQueue
     * is arrivedTasks queue implementation.
     */
    public DefaultTransactionSelector() {
        this(new LinkedBlockingQueue<K>());
    }

    /**
     * Creates a new TransactionSelector with the provided pending task queue.
     *
     * @param arrivedTasks the BlockingQueue where the Trans
     * @throws NullPointerException if arrivedTasks is null.
     */
    public DefaultTransactionSelector(BlockingQueue<K> arrivedTasks) {
        if (arrivedTasks == null) {
            throw new NullPointerException();
        }
        this.arrivedTasks = arrivedTasks;
    }

    @Override
    public void register(K task) {
        if (task == null) {
            throw new NullPointerException();
        }

        Transaction tx = task.getTransaction();
        tx.registerRetryLatch(new NonBlockingLatch(task));
    }

    @Override
    public K select() throws InterruptedException {
        return arrivedTasks.take();
    }

    @Override
    public K selectNow() {
        throw new TodoException();
    }

    @Override
    public void close() {
        throw new TodoException();
    }

    private class NonBlockingLatch implements Latch {

        private final AtomicBoolean open = new AtomicBoolean();
        private final K context;

        public NonBlockingLatch(K context) {
            this.context = context;
        }

        @Override
        public void open() {
            if (open.compareAndSet(false, true)) {
                try {
                    arrivedTasks.put(context);
                } catch (InterruptedException e) {
                    //todo:
                    e.printStackTrace();
                }
            }
        }

        @Override
        public boolean isOpen() {
            return open.get();
        }

        @Override
        public void await() throws InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void awaitUninterruptible() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean tryAwait(long timeout, TimeUnit unit) throws InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean tryAwaitUninterruptible(long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean tryAwaitNs(long timeoutNs) throws InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean tryAwaitUninterruptibleNs(long timeout) {
            throw new UnsupportedOperationException();
        }
    }
}
