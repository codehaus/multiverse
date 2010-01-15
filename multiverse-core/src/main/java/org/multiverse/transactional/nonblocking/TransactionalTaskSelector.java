package org.multiverse.transactional.nonblocking;

import org.multiverse.utils.TodoException;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TransactionalTaskSelector {

    private final BlockingQueue<TransactionalTask> pendingTasks;

    public TransactionalTaskSelector() {
        this(new LinkedBlockingQueue<TransactionalTask>());
    }

    public TransactionalTaskSelector(BlockingQueue<TransactionalTask> pendingTasks) {
        if (pendingTasks == null) {
            throw new NullPointerException();
        }
        this.pendingTasks = pendingTasks;
    }

    public void bla(TransactionalTask task) {
        if (task == null) {
            throw new NullPointerException();
        }


    }

    /**
     * Selects a TransactionalTask ready for execution or wait until a task comes available.
     *
     * @return the TransactionalTask that is ready for execution.
     * @throws InterruptedException if the thread is interrupted while waiting for a task to come available.
     */
    public TransactionalTask select() throws InterruptedException {
        return pendingTasks.take();
    }

    public TransactionalTask selectNow() {
        throw new TodoException();
    }

    public TransactionalTask select(long timeout, TimeUnit unit) {
        throw new TodoException();
    }
}
