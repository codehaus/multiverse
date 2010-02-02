package org.multiverse.api;

import java.util.concurrent.TimeUnit;

/**
 * A structure that can be used as a waiting point (just like a {@link java.util.concurrent.Future} or a
 * {@link java.util.concurrent.CountDownLatch}. As long as it is closed, the waiting thread block (unless it is
 * interrupted or a timeout occurs). As soon as it opens, all waiting threads are woken up and continue.
 * Threads that call the wait after the Latch is opened, can continue. Once the Latch has been opened, it can
 * never be closed.
 * <p/>
 * A Latch is thread-safe to use.
 * <p/>
 *
 * @author Peter Veentjer.
 */
public interface Latch {

    /**
     * Opens the latch. If the latch already is open, the call is ignored. So this call is idempotent.
     */
    void open();

    /**
     * Return true if this Latch is open, false otherwise.
     *
     * @return true if this Latch is open, false otherwise.
     */
    boolean isOpen();

    /**
     * Waits for this Latch to open. If the Latch already is open, the call continues.
     * <p/>
     * If the Latch already is open, the call continues. It depends on the implementation if the InterruptedException
     * is thrown in this case if the calling thread is interrupted (so the interrupted flag is set).
     *
     * @throws InterruptedException if the calling thread is interrupted while waiting.
     */
    void await() throws InterruptedException;

    /**
     * Waits for this Latch to open and while waiting it won't be interrupted.
     * <p/>
     * If the thread is interrupted while waiting, the InterruptedException is dropped and the interrupt status is
     * restored as soon as the method returns (so it won't be eaten).
     * <p/>
     * If the Latch already is open, the call continues. It depends on the implementation if the InterruptedException
     * is thrown in this case if the calling thread is interrupted (so the interrupted flag is set).
     *
     * @throws UnsupportedOperationException if the implementation doesn't support this functionality.
     */
    void awaitUninterruptible();

    /**
     * Waits for this Latch to open or till a timeout occurs or when the calling thread is interrupted.
     * <p/>
     * If the Latch already is open, the call continues. It depends on the implementation if the InterruptedException
     * is thrown in this case if the calling thread is interrupted (so the interrupted flag is set).
     *
     * @param timeout the maximum time to wait.
     * @param unit    the TimeUnit the timeout is expressed in
     * @return true if the lock is open, false otherwise.
     * @throws InterruptedException          if the calling thread is interrupted while waiting.
     * @throws NullPointerException          if  unit is null
     * @throws UnsupportedOperationException if the implementation doesn't support this functionality.
     */
    boolean tryAwait(long timeout, TimeUnit unit) throws InterruptedException;


    boolean tryAwaitUninterruptible(long timeout, TimeUnit unit);
}
