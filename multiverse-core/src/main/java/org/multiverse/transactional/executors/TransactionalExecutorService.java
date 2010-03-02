package org.multiverse.transactional.executors;

import org.multiverse.annotations.TransactionalMethod;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A transactional version of the {@link ExecutorService}.
 *
 * @author Peter Veentjer
 */
public interface TransactionalExecutorService extends TransactionalExecutor, ExecutorService {

    @Override
    @TransactionalMethod(readonly = true)
    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

    @Override
    @TransactionalMethod(readonly = true)
    boolean isShutdown();

    @Override
    @TransactionalMethod(readonly = true)
    boolean isTerminated();
}
