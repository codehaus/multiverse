package org.multiverse.api.blocking;

import java.util.concurrent.TimeUnit;

/**
 * A blockingAllowed structure that can be used to create blockingAllowed transactions. When a transaction blocks, a 'listener' is
 * added to each read transactional object. This listener is the Latch. Each transactional object can have a set
 * of listeners, see the {@link Listeners} for more information.
 * <p/>
 * The Latch can safely be garbage collected because it works based on an listenerEra. When a transaction wants to block
 * it gets a Latch from the pool and reset it so it can be used. By resetting it, the listenerEra-counter is incremented,
 * so that call to open or await are ignored.
 *
 * @author Peter Veentjer.
 */
public interface Latch {

    /**
     * Awaits this latch to go open.
     *
     * @param expectedEra the expected era. If the era is different, the await always succeeds.
     */
    void awaitUninterruptible(long expectedEra);

    void await(long expectedEra) throws InterruptedException;

    long tryAwaitUninterruptible(long expectedEra, long timeout, TimeUnit unit);

    long tryAwait(long expectedEra, long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Checks if the Latch is open.
     *
     * @return true if the Latch is open, false otherwise.
     */
    boolean isOpen();

    /**
     * Opens this latch only if the expectedEra is the same. If the expectedEra is not the same, the call is ignored.
     *
     * @param expectedEra
     */
    void open(long expectedEra);

    /**
     * Prepares the Latch for pooling. All waiting threads will be notified and the era is increased.
     */
    void prepareForPooling();

    long getEra();
}
