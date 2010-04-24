package org.multiverse.api.backoff;

import org.multiverse.api.Transaction;

/**
 * A {@link BackoffPolicy} that does not backoff. This implementation
 * is useful a BackoffPolicy instance is needed, but no delay should be done. It is unlikely that
 * you want to have this policy in production.
 *
 * @author Peter Veentjer.
 */
public final class NoOpBackoffPolicy implements BackoffPolicy {

    public final static NoOpBackoffPolicy INSTANCE = new NoOpBackoffPolicy();

    @Override
    public void delay(Transaction t) throws InterruptedException {
        //do nothing
    }

    @Override
    public void delayedUninterruptible(Transaction t) {
        //do nothing
    }
}
