package org.multiverse.stms.beta;

import org.multiverse.api.BackoffPolicy;
import org.multiverse.api.blocking.CheapLatch;
import org.multiverse.api.blocking.StandardLatch;
import org.multiverse.api.exceptions.*;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.beta.ThreadLocalBetaObjectPool.getThreadLocalBetaObjectPool;

/**
 * @author Peter Veentjer
 */
public abstract class BetaTransactionTemplate<E> {

    public long controlFlowErrorCount = 0;
    public long readConflictCount = 0;
    public long writeConflictCount = 0;

    private final BetaTransactionFactory transactionFactory;

    public BetaTransactionTemplate(BetaStm stm) {
        this(stm.getTransactionFactoryBuilder().build());
    }

    public BetaTransactionTemplate(BetaTransactionFactory transactionFactory) {
        if (transactionFactory == null) {
            throw new NullPointerException();
        }
        this.transactionFactory = transactionFactory;
    }

    public abstract E execute(BetaTransaction tx) throws Exception;

    public final E execute() {
        return execute(getThreadLocalBetaObjectPool());
    }

    public final E executeChecked() throws Exception {
        return executeChecked(getThreadLocalBetaObjectPool());
    }

    public final E execute(BetaObjectPool pool) {
        try {
            return executeChecked(pool);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new InvisibleCheckedException(e);
            }
        }
    }

    public final E executeChecked(BetaObjectPool pool) throws Exception {
        if (pool == null) {
            throw new NullPointerException();
        }

        BetaTransaction tx = (BetaTransaction) getThreadLocalTransaction();
        if (tx != null && !tx.getStatus().isAlive()) {
            tx = null;
        }

        switch (transactionFactory.getTransactionConfiguration().getPropagationLevel()) {
            case Requires:
                if (tx == null) {
                    tx = transactionFactory.start(pool);
                    setThreadLocalTransaction(tx);
                    return doWithTransaction(tx, pool);
                } else {
                    return execute(tx);
                }
            case Mandatory:
                if (tx == null) {
                    throw new NoTransactionFoundException();
                }
                tx = transactionFactory.start(pool);
                setThreadLocalTransaction(tx);
                return doWithTransaction(tx, pool);
            case Never:
                if (tx != null) {
                    throw new NoTransactionAllowedException();
                }
                return execute();
            case RequiresNew:
                if (tx == null) {
                    tx = transactionFactory.start(pool);
                    setThreadLocalTransaction(tx);
                    return doWithTransaction(tx, pool);
                } else {
                    BetaTransaction suspendedTransaction = tx;
                    tx = transactionFactory.start();
                    setThreadLocalTransaction(tx);
                    try {
                        return doWithTransaction(tx, pool);
                    } finally {
                        setThreadLocalTransaction(suspendedTransaction);
                    }
                }
            case Supports:
                return execute(tx);
            default:
                throw new IllegalStateException();
        }
    }

    private E doWithTransaction(BetaTransaction tx, BetaObjectPool pool) throws Exception {
        BackoffPolicy backoffPolicy = tx.getConfiguration().getBackoffPolicy();

        boolean abort = true;

        try {
            do {
                try {
                    E value = execute(tx);
                    tx.commit(pool);
                    abort = false;
                    return value;
                } catch (Retry e) {
                    controlFlowErrorCount++;
                    waitForChange(pool, tx);
                    abort = false;
                } catch (SpeculativeConfigurationError e) {
                    controlFlowErrorCount++;
                    BetaTransaction old = tx;
                    tx = transactionFactory.upgradeAfterSpeculativeFailure(tx, pool);
                    pool.putBetaTransaction(old);
                    abort = false;
                } catch (ReadConflict e) {
                    readConflictCount++;
                    controlFlowErrorCount++;
                    backoffPolicy.delayedUninterruptible(tx.getAttempt());
                    abort = false;
                } catch (WriteConflict e) {
                    controlFlowErrorCount++;
                    writeConflictCount++;
                    backoffPolicy.delayedUninterruptible(tx.getAttempt());
                    abort = false;
                }
            } while (tx.softReset(pool));
        } finally {
            if (abort) {
                tx.abort(pool);
            }

            pool.putBetaTransaction(tx);
            clearThreadLocalTransaction();
        }

        String msg = format("Maximum number of %s retries has been reached for transaction '%s'",
                tx.getConfiguration().getMaxRetries(), tx.getConfiguration().getFamilyName());
        throw new TooManyRetriesException(msg);
    }

    private void waitForChange(BetaObjectPool pool, BetaTransaction tx) throws InterruptedException {
        if (tx.getConfiguration().getTimeoutNs() == Long.MAX_VALUE) {
            waitForChangeWithoutTimeout(pool, tx);
        } else {
            waitForChangeWithTimeout(pool, tx);
        }
    }

    private void waitForChangeWithTimeout(BetaObjectPool pool, BetaTransaction tx) throws InterruptedException {
        StandardLatch latch = pool.takeStandardLatch();
        long lockEra;
        if (latch == null) {
            latch = new StandardLatch();
        }

        lockEra = latch.getEra();

        tx.registerChangeListenerAndAbort(latch, pool);

        try {
            long timeoutNs = tx.getRemainingTimeoutNs();

            if (tx.getConfiguration().isInterruptible()) {
                timeoutNs = latch.tryAwait(lockEra, timeoutNs, TimeUnit.NANOSECONDS);
            } else {
                timeoutNs = latch.tryAwaitUninterruptible(lockEra, timeoutNs, TimeUnit.NANOSECONDS);
            }

            tx.setRemainingTimeoutNs(timeoutNs);
            if (timeoutNs < 0) {
                String msg = format("Transaction %s has timed with a total timeout of %s ns",
                        tx.getConfiguration().getFamilyName(),
                        tx.getConfiguration().getTimeoutNs());
                throw new RetryTimeoutException(msg);
            }
        } finally {
            pool.putStandardLatch(latch);
        }
    }

    private void waitForChangeWithoutTimeout(BetaObjectPool pool, BetaTransaction tx) throws InterruptedException {
        CheapLatch latch = pool.takeCheapLatch();
        long lockEra;
        if (latch == null) {
            latch = new CheapLatch();
        }

        lockEra = latch.getEra();

        tx.registerChangeListenerAndAbort(latch, pool);
        try {
            if (tx.getConfiguration().isInterruptible()) {
                latch.await(lockEra);
            } else {
                latch.awaitUninterruptible(lockEra);
            }
        } finally {
            pool.putCheapLatch(latch);
        }
    }
}
