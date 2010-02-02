package org.multiverse.transactional.nonblocking;

/**
 * A TransactionSelector can be compared to the {@link java.nio.channels.Selector}. It makes it possible that
 * multiple blocking transactions share the same executing thread. The advantage is that because no thread per
 * blocking transaction is needed, the number of threads is reduced. In theory you could have millions of blocking
 * transactions being executed by a few hundred threads. Of course the advantage of using this approach depends on
 * the amount of blocking done.
 *
 * @author Peter Veentjer.
 */
public interface TransactionSelector<K extends TransactionSelectionKey> {

    /**
     * Registers a NonBlockingTaskContext to this TransactionSelector so that it
     *
     * No guarantees are made of the same NonBlockingTaskContext is registered while it already is registered
     * (and not been returned with a select).
     *
     * @param key
     * @throws NullPointerException if key is null.
     */
    void register(K key);

    /**
     * Selects a NonBlockingTaskContext ready for execution or wait until a task comes available.
     * <p/>
     * Once the NonBlockingTaskContext is returned, it needs to be registered again if it wants to participate
     * in another attempt.
     *
     * @return the NonBlockingTask that is ready for execution.
     * @throws InterruptedException if the thread is interrupted while waiting for a task to come available.
     */
    K select() throws InterruptedException;

    K selectNow();

    void close();
}
