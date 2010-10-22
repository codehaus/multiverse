package org.multiverse.api.blocking;

import java.util.concurrent.TimeUnit;

/**
 * A blockingAllowed structure that can be used to create blockingAllowed transactions. When a transaction blocks, a 'listener' is
 * added to each read transactional object. This listener is the Latch. Each transactional object can have a getAndSet
 * of listeners, see the {@link org.multiverse.stms.beta.Listeners} for more information.
 * <p/>
 * The Latch can safely be garbage collected because it works based on an listenerEra. When a transaction wants to block
 * it gets a Latch from the pool and reset it so it can be used. By resetting it, the listenerEra-counter is incremented,
 * so that call to open or await are ignored.
 *
 * @author Peter Veentjer.
 */
public interface Latch {

    /**
     * Awaits for this latch to open. This call is not responsive to interrupts.
     *
     * @param expectedEra the expected era. If the era is different, the await always succeeds.
     */
    void awaitUninterruptible(long expectedEra);

    /**
     * Awaits for this Latch to open. There are 3 possible ways for this methods to complete;
     * <ol>
     * <li>the era doesn't match the expected era</li>
     * <li>the latch is opened while waiting</li>
     * <li>the latch is interrupted while waiting</li>
     * </ol>
     *
     * @param expectedEra the expected era.
     * @throws org.multiverse.api.exceptions.TransactionInterruptedException
     */
    void await(long expectedEra);

    /**
     * Awaits for this latch to open with a timeout. This call is not responsive to interrupts.
     *
     * @param expectedEra the expected era.
     * @param timeout     the timeout
     * @param unit        the TimeUnit for the timeout
     * @return the remaining timeout.  A negative value indicates that the Latch is not opened in time.
     * @throws NullPointerException          if unit is null
     */
    long tryAwaitUninterruptible(long expectedEra, long timeout, TimeUnit unit);

    long tryAwaitUninterruptible(long expectedEra, long timeoutNs);

    /**
     * Awaits for this latch to open with a timeout. This call is responsive to interrupts.
     *
     * @param expectedEra the expected era
     * @param timeout     the timeout
     * @param unit        the TimeUnit for the timeout
     * @return the remaining timeout. A negative value indicates that the latch is not opened in time.
     * @throws org.multiverse.api.exceptions.TransactionInterruptedException
     * @throws NullPointerException          if unit is null
     */
    long tryAwait(long expectedEra, long timeout, TimeUnit unit);

    long tryAwait(long expectedEra, long timeoutNs);

    /**
     * Checks if the Latch is open.
     *
     * @return true if the Latch is open, false otherwise.
     */
    boolean isOpen();

    /**
     * Opens this latch only if the expectedEra is the same. If the expectedEra is not the same, the call is ignored.
     * If the Latch already is open, this call is also ignored.
     *
     * @param expectedEra the expected era.
     */
    void open(long expectedEra);

    /**
     * Prepares the Latch for pooling. All waiting threads will be notified and the era is increased.
     */
    void prepareForPooling();

    /**
     * Gets the current era.
     *
     * @return the current era.
     */
    long getEra();
}
