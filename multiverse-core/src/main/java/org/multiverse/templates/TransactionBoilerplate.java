package org.multiverse.templates;

import org.multiverse.MultiverseConstants;
import org.multiverse.api.*;
import org.multiverse.api.backoff.BackoffPolicy;
import org.multiverse.api.exceptions.*;
import org.multiverse.api.latches.CheapLatch;
import org.multiverse.api.latches.Latch;
import org.multiverse.api.latches.StandardLatch;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;

import static java.lang.String.format;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * TransactionBoilerplate contains the boilerplate logic to deal with transaction and needs to
 * be used in combination with the TransactionalCallable.
 * <p/>
 * example:
 * <pre>
 * new TransactionLogic().execute(new TransactionalCallable<Integer> {
 *         Integer call(){
 *            return queue.pop();
 *         }
 * }).execute();
 * </pre>
 * <p/>
 * TransactionLogic is threadsafe and is designed to be reused. In case of transactional actors,
 * you can create a TransactionLogic object per class of actors (so static field) or per actor
 * (instance field).
 *
 * @author Sai Venkat
 * @author Peter Veentjer.
 */
public final class TransactionBoilerplate implements MultiverseConstants {

    private final boolean threadLocalAware;
    private final TransactionFactory transactionFactory;
    private final TransactionLifecycleListener listener;

    /**
     * Creates a new TransactionLogic that uses the STM stored in the GlobalStmInstance and works the the {@link
     * org.multiverse.api.ThreadLocalTransaction}. It automatically creates update transactions that are able to track
     * reads.
     * <p/>
     * This constructor should only be used for experimentation purposes. If you want something fast, you need to pass
     * in a TransactionFactory.
     */
    public TransactionBoilerplate() {
        this(getGlobalStmInstance());
    }

    /**
     * Creates a new TransactionLogic using the provided STM. The transaction used is stores/retrieved from the
     * {@link org.multiverse.api.ThreadLocalTransaction}.
     * <p/>
     * It automatically creates read tracking update transactions.
     * <p/>
     * This constructor should only be used for experimentation purposes. If you want something fast, you need to pass
     * in a TransactionFactory.
     *
     * @param stm the stm to use for transactions.
     * @throws NullPointerException if stm is null.
     */
    public TransactionBoilerplate(Stm stm) {
        this(stm.getTransactionFactoryBuilder()
                .setReadonly(false)
                .setReadTrackingEnabled(true)
                .build());
    }

    /**
     * Creates a new TransactionLogic with the provided TransactionFactory and that is aware of the
     * ThreadLocalTransaction.
     *
     * @param transactionFactory the TransactionFactory used to createReference Transactions.
     * @throws NullPointerException if txFactory is null.
     */
    public TransactionBoilerplate(TransactionFactory transactionFactory) {
        this(transactionFactory, null, true);
    }


    /**
     * Creates a new TransactionLogic with the provided TransactionFactory.
     *
     * @param transactionFactory the TransactionFactory used to createReference Transactions.
     * @param threadLocalAware   true if this TransactionLogic should look at the ThreadLocalTransaction
     *                           and publish the running transaction there.
     *                           todo
     * @param listener           true if the lifecycle callback methods should be enabled. Enabling them causes
     *                           extra object creation, so if you want to squeeze out more performance, set
     *                           this to false at the price of not having the lifecycle methods working.
     */
    public TransactionBoilerplate(
            TransactionFactory transactionFactory, TransactionLifecycleListener listener, boolean threadLocalAware) {
        if (transactionFactory == null) {
            throw new NullPointerException();
        }

        this.listener = listener;
        this.transactionFactory = transactionFactory;
        this.threadLocalAware = threadLocalAware;
    }

    /**
     * Checks if the TransactionLogic should work together with the ThreadLocalTransaction.
     *
     * @return true if the TransactionLogic should work together with the ThreadLocalTransaction.
     */
    public boolean isThreadLocalAware() {
        return threadLocalAware;
    }

    /**
     * Returns the {@link org.multiverse.api.lifecycle.TransactionLifecycleListener}
     *
     * @return the TransactionLifecycleListener. Is allowed to be null which indicates that no events
     *         will be send.
     */
    public TransactionLifecycleListener getTransactionLifecycleListener() {
        return listener;
    }

    /**
     * Returns the TransactionFactory this TransactionLogic uses to createReference Transactions.
     *
     * @return the TransactionFactory this TransactionLogic uses to createReference Transactions.
     */
    public TransactionFactory getTransactionFactory() {
        return transactionFactory;
    }

    /**
     * Lifecycle method that is called every time the transaction is started. It could be that a transaction is retried
     * (for example because of a read or write conflict or because of a blocking transaction).
     * <p/>
     * If this TransactionLogic doesn't starts its own transaction, this method won't be called.
     * <p/>
     * If an exception is thrown while executing this method, the execute and transaction will abort.
     *
     * @param tx the Transaction that is started.
     */
    protected void onStart(Transaction tx) {
    }

    /**
     * Executes the TransactionalCallable. If the callable throws a checked exception, this exception
     * is wrapped inside a {@link org.multiverse.templates.InvisibleCheckedException}.
     *
     * @param callable the TransactionalCallable to execute.
     * @return the result.
     * @throws InvisibleCheckedException if a checked exception was thrown while executing the callable.
     * @throws org.multiverse.api.exceptions.DeadTransactionException
     *                                   if the exception was explicitly aborted.
     * @throws org.multiverse.api.exceptions.TooManyRetriesException
     *                                   if the template retried the transaction too many times. The cause of the last
     *                                   failure (also an exception) is included as cause. So you have some idea where
     *                                   to look for problems
     * @throws NullPointerException      if callable is null.
     */
    public <E> E execute(TransactionalCallable<E> callable) {
        if (callable == null) {
            throw new NullPointerException("callable can't be null");
        }

        try {
            return executeChecked(callable);
        } catch (Exception ex) {
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else {
                throw new InvisibleCheckedException(ex);
            }
        }
    }

    /**
     * Executes the Template.
     *
     * @param callable the callable to execute.
     * @return the result
     * @throws org.multiverse.api.exceptions.DeadTransactionException
     *                              if the exception was explicitly aborted.
     * @throws org.multiverse.api.exceptions.TooManyRetriesException
     *                              if the template retried the transaction too many times. The cause of the last
     *                              failure (also an exception) is included as cause. So you have some idea where to
     *                              look for problems
     * @throws NullPointerException if callable is null.
     * @throws Exception            if the callable throws an exception.
     */
    public <E> E executeChecked(TransactionalCallable<E> callable) throws Exception {
        if (callable == null) {
            throw new NullPointerException("callable can't be null");
        }

        Transaction tx = threadLocalAware ? getThreadLocalTransaction() : null;

        if (isDead(tx)) {
            if (sameTransactionFactory(tx)) {
                tx.reset();
                tx.setAttempt(0);
                tx.setRemainingTimeoutNs(tx.getConfiguration().getTimeoutNs());
            } else {
                tx = transactionFactory.create();
            }

            if (threadLocalAware) {
                setThreadLocalTransaction(tx);
            }

            return executeWithTransaction(tx, callable);
        } else {
            return callable.call(tx);
        }
    }

    private boolean sameTransactionFactory(Transaction tx) {
        return tx != null && tx.getTransactionFactory() == transactionFactory;
    }

    private <E> E executeWithTransaction(Transaction tx, TransactionalCallable<E> callable) throws Exception {
        Throwable lastFailureCause = null;

        if (listener == null) {
            //todo
            //listener.notify();
        }

        try {
            do {
                tx.setAttempt(tx.getAttempt() + 1);
                try {
                    try {
                        logStart(tx);

                        if (listener != null) {
                            tx.registerLifecycleListener(listener);
                        }

                        onStart(tx);
                        E result = callable.call(tx);
                        tx.commit();
                        return result;
                    } catch (Retry e) {
                        handleRetry(tx);
                    }
                } catch (SpeculativeConfigurationFailure ex) {
                    tx = handleSpeculativeConfigurationFailure(tx);
                } catch (ControlFlowError er) {
                    handleControlFlowError(tx, er);
                }
            } while (tx.getAttempt() - 1 < tx.getConfiguration().getMaxRetries());

            String msg = format("Too many retries on transaction '%s', maxRetries = %s",
                    tx.getConfiguration().getFamilyName(),
                    tx.getConfiguration().getMaxRetries());
            throw new TooManyRetriesException(msg, lastFailureCause);
        } finally {
            if (tx != null && tx.getStatus() != TransactionStatus.Committed) {
                tx.abort();
            }
        }
    }

    private static void logStart(Transaction tx) {
        if (___LOGGING_ENABLED) {
            if (tx.getConfiguration().getLogLevel().isLogableFrom(LogLevel.course)) {
                System.out.println(tx.getConfiguration().getFamilyName() + " starting");
            }
        }
    }

    private static void handleControlFlowError(Transaction tx, ControlFlowError er) {
        if (___LOGGING_ENABLED) {
            if (tx.getConfiguration().getLogLevel().isLogableFrom(LogLevel.course)) {
                System.out.println(tx.getConfiguration().getFamilyName() + " " + er.getDescription());
            }
        }

        BackoffPolicy backoffPolicy = tx.getConfiguration().getBackoffPolicy();
        backoffPolicy.delayedUninterruptible(tx);
        tx.reset();
    }

    private Transaction handleSpeculativeConfigurationFailure(Transaction oldTx) {
        if (___LOGGING_ENABLED) {
            if (oldTx.getConfiguration().getLogLevel().isLogableFrom(LogLevel.course)) {
                System.out.println(oldTx.getConfiguration().getFamilyName() + " speculative configuration failure");
            }
        }


        oldTx.abort();
        Transaction newTx = transactionFactory.create();
        newTx.setAttempt(oldTx.getAttempt());
        newTx.setRemainingTimeoutNs(oldTx.getRemainingTimeoutNs());

        if (threadLocalAware) {
            setThreadLocalTransaction(newTx);
        }
        return newTx;
    }

    private static void handleRetry(Transaction tx) throws InterruptedException {
        if (___LOGGING_ENABLED) {
            if (tx.getConfiguration().getLogLevel().isLogableFrom(LogLevel.course)) {
                System.out.println(tx.getConfiguration().getFamilyName() + " retry (blocking)");
            }
        }

        if (tx.getAttempt() - 1 < tx.getConfiguration().getMaxRetries()) {

            Latch latch;
            if (tx.getRemainingTimeoutNs() == Long.MAX_VALUE) {
                latch = new CheapLatch();
            } else {
                latch = new StandardLatch();
            }

            tx.registerRetryLatch(latch);
            tx.abort();

            if (tx.getRemainingTimeoutNs() == Long.MAX_VALUE) {
                if (tx.getConfiguration().isInterruptible()) {
                    latch.await();
                } else {
                    latch.awaitUninterruptible();
                }
            } else {
                long beginNs = System.nanoTime();

                boolean timeout;
                if (tx.getConfiguration().isInterruptible()) {
                    timeout = !latch.tryAwaitNs(tx.getRemainingTimeoutNs());
                } else {
                    timeout = !latch.tryAwaitNs(tx.getRemainingTimeoutNs());
                }

                long durationNs = System.nanoTime() - beginNs;
                tx.setRemainingTimeoutNs(tx.getRemainingTimeoutNs() - durationNs);

                if (timeout) {
                    String msg = format("Transaction %s has timed with a total timeout of %s ns",
                            tx.getConfiguration().getFamilyName(),
                            tx.getConfiguration().getTimeoutNs());
                    throw new RetryTimeoutException(msg);
                }
            }

            tx.reset();
        }
    }

    private static boolean isDead(Transaction t) {
        return t == null || t.getStatus().isDead();
    }
}
