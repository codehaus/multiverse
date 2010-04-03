package org.multiverse.api.backoff;

import org.multiverse.api.Transaction;

/**
 * A {@link BackoffPolicy} that does not backoff. This implementation
 * is useful a BackoffPolicy instance is needed, but no delay should be executed.
 *
 * @author Peter Veentjer.
 */
public class NoOpBackoffPolicy implements BackoffPolicy {

    public final static NoOpBackoffPolicy INSTANCE = new NoOpBackoffPolicy();

    @Override
    public void delay(Transaction t, int attempt) throws InterruptedException {
        //do nothing
    }

    @Override
    public void delayedUninterruptible(Transaction t, int attempt) {
        //do nothing
    }
}
