package org.multiverse.stms.beta;

import org.multiverse.api.AtomicBlock;
import org.multiverse.api.BackoffPolicy;
import org.multiverse.api.blocking.CheapLatch;
import org.multiverse.api.blocking.StandardLatch;
import org.multiverse.api.exceptions.RetryTimeoutException;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.BetaTransactionConfiguration;

import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

public abstract class AbstractBetaAtomicBlock implements AtomicBlock {

    protected final BetaTransactionFactory transactionFactory;
    protected final BetaTransactionConfiguration transactionConfiguration;
    protected final BackoffPolicy backoffPolicy;
    protected final boolean hasTimeout;

    public AbstractBetaAtomicBlock(final BetaTransactionFactory transactionFactory) {
        if (transactionFactory == null) {
            throw new NullPointerException();
        }
        this.transactionFactory = transactionFactory;
        this.transactionConfiguration = transactionFactory.getTransactionConfiguration();
        this.backoffPolicy = transactionConfiguration.backoffPolicy;
        this.hasTimeout = transactionConfiguration.hasTimeout();
    }

    protected final void waitForChange(final BetaTransaction tx) throws InterruptedException {

        if (hasTimeout) {
            waitForChangeWithTimeout(tx);
        } else {
            waitForChangeWithoutTimeout(tx);
        }
    }

    protected final void waitForChangeWithTimeout(
            final BetaTransaction tx) throws InterruptedException {

        BetaObjectPool pool = tx.getPool();
        StandardLatch latch = pool.takeStandardLatch();
        if (latch == null) {
            latch = new StandardLatch();
        }

        final long lockEra = latch.getEra();

        try {
            tx.registerChangeListenerAndAbort(latch);

            long timeoutNs = tx.getRemainingTimeoutNs();

            if (transactionConfiguration.isInterruptible()) {
                timeoutNs = latch.tryAwait(lockEra, timeoutNs, TimeUnit.NANOSECONDS);
            } else {
                timeoutNs = latch.tryAwaitUninterruptible(lockEra, timeoutNs, TimeUnit.NANOSECONDS);
            }

            tx.setRemainingTimeoutNs(timeoutNs);
            if (timeoutNs < 0) {
                throw new RetryTimeoutException(
                        format("Transaction %s has timed with a total timeout of %s ns",
                                transactionConfiguration.getFamilyName(),
                                transactionConfiguration.getTimeoutNs()));
            }
        } finally {
            pool.putStandardLatch(latch);
        }
    }

    protected final void waitForChangeWithoutTimeout(final BetaTransaction tx) throws InterruptedException {
        BetaObjectPool pool = tx.getPool();
        CheapLatch latch = pool.takeCheapLatch();
        if (latch == null) {
            latch = new CheapLatch();
        }

        final long lockEra = latch.getEra();

        try {
            tx.registerChangeListenerAndAbort(latch);
            if (transactionConfiguration.isInterruptible()) {
                latch.await(lockEra);
            } else {
                latch.awaitUninterruptible(lockEra);
            }
        } finally {
            pool.putCheapLatch(latch);
        }
    }
}
