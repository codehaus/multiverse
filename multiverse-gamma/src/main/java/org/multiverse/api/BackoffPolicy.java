package org.multiverse.api;

/**
 * A policy to be used when the system can't make any progress, e.g. caused by a ReadWriteConflict, and it is better
 * to wait some time so the chance of success increases.
 *
 * @author Peter Veentjer.
 */
public interface BackoffPolicy {

    void delay(int attempt) throws InterruptedException;

    void delayUninterruptible(int attempt);
}
