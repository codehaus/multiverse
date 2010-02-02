package org.multiverse.transactional.nonblocking;

import org.multiverse.annotations.TransactionalObject;

/**
 * An NonBlockingTransactionExecutor can be compared to the {@link java.util.concurrent.Executor} except
 * that this executor executes non blocking tasks.
 *
 *
 *
 * @author Peter Veentjer.
 */
@TransactionalObject
public interface NonBlockingTransactionExecutor {

    /**
     * Executes a NonBlockingTask.
     *
     * @param task
     * @throws NullPointerException if task is null.
     */
    void execute(NonBlockingTask task);
}
