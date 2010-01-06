package org.multiverse.utils.restartbackoff;

import org.multiverse.api.Transaction;

/**
 * A {@link org.multiverse.utils.restartbackoff.RestartBackoffPolicy} that does not restartbackoff. This implementation
 * is useful a RestartBackoffPolicy instance is needed, but no delay should be executed.
 *
 * @author Peter Veentjer.
 */
public class NoOpRestartBackoffPolicy implements RestartBackoffPolicy {

    public final static NoOpRestartBackoffPolicy INSTANCE = new NoOpRestartBackoffPolicy();

    @Override
    public void delay(Transaction t, int attempt) throws InterruptedException {
        //do nothing
    }

    @Override
    public void delayUninterruptible(Transaction t, int attempt) {
        //do nothing
    }
}
