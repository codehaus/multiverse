package org.multiverse.stms.alpha.instrumentation.transactionalmethod;

import org.multiverse.api.TransactionFactory;
import org.multiverse.api.TransactionStatus;
import org.multiverse.api.backoff.BackoffPolicy;
import org.multiverse.api.exceptions.*;
import org.multiverse.api.latches.CheapLatch;
import org.multiverse.api.latches.Latch;
import org.multiverse.api.latches.StandardLatch;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static java.lang.String.format;
import static org.multiverse.api.ThreadLocalTransaction.*;

/**
 * The donor class that can be used while instrumenting atomic methods and adding the transaction management
 * donorMethod.
 * <p/>
 * It is important that if there are static methods used in this donor that they are public, so that they still can be
 * used if the donor methods are transplanted.
 *
 * @author Peter Veentjer.
 */
public class TransactionLogicDonor {

    public static void execute___ro() {
    }

    public static void execute___up() {
    }


    // =================== for constructors ====================

    //fields can't be made final, the compiler could inline the values so can't be replaced by instrumentation.
    //By the instrumentation these static fields will be replaced by the actual TransactionalMethod parameters
    public static TransactionFactory transactionFactory;

    public static void donorConstructor() throws Exception {
        AlphaTransaction tx = (AlphaTransaction) getThreadLocalTransaction();

        if (isActiveTransaction(tx)) {
            execute___up();
            return;
        }

        tx = createTransaction(tx, transactionFactory);
        tx.setAttempt(1);
        setThreadLocalTransaction(tx);
        try {
            execute___ro();
            tx.commit();
            tx = null;
        } catch (Throwable throwable) {
            tx.abort();

            if (throwable instanceof ControlFlowError) {
                throw new ConstructorCantRetryException();
            } else if (throwable instanceof Error) {
                throw (Error) throwable;
            } else {
                throw (Exception) throwable;
            }
        } finally {
            clearThreadLocalTransaction();
        }
    }

    // ============ for transactional getters and setters ================
/*
    public <E> E donorGet() {
        Transaction tx = getThreadLocalTransaction();

        if (tx == null || tx.getStatus().isDead()) {
            return atomicGet();
        }

        return donorGet(tx);
    }

    public <E> E donorGet(Transaction tx) {
        if (tx == null) {
            throw new NullPointerException();
        }

        AlphaProgrammaticRefTranlocal<E> tranlocal = openForRead(tx);
        return tranlocal.value;
    }

    public <E> E atomicGet() {
        AlphaProgrammaticRefTranlocal<E> tranlocal = (AlphaProgrammaticRefTranlocal) ___load();

        if (tranlocal == null) {
            throw new UncommittedReadConflict();
        }

        return tranlocal.value;
    }
   */
    // ================ for other transactional methods ===============


    public static void donorMethod() throws Exception {
        AlphaTransaction tx = (AlphaTransaction) getThreadLocalTransaction();

        if (isActiveTransaction(tx)) {
            if (tx.getConfiguration().isReadonly()) {
                execute___ro();
            } else {
                execute___up();
            }
            return;
        }

        tx = createTransaction(tx, transactionFactory);

        setThreadLocalTransaction(tx);
        try {
            do {
                try {
                    try {
                        tx.setAttempt(tx.getAttempt() + 1);
                        if (tx.getConfiguration().isReadonly()) {
                            execute___ro();
                        } else {
                            execute___up();
                        }

                        tx.commit();
                        return;
                    } catch (Retry er) {
                        handleRetry(tx);
                    }
                } catch (SpeculativeConfigurationFailure speculativeConfigurationFailure) {
                    tx = handleSpeculativeFailure(transactionFactory, tx);
                } catch (ControlFlowError controlFlowError) {
                    BackoffPolicy backoffPolicy = tx.getConfiguration().getBackoffPolicy();
                    backoffPolicy.delayedUninterruptible(tx);
                } finally {
                    if (tx.getStatus() != TransactionStatus.Committed && tx.getAttempt() - 1 < tx.getConfiguration().getMaxRetries()) {
                        tx.reset();
                    }
                }
            } while (tx.getAttempt() - 1 < tx.getConfiguration().getMaxRetries());

            String msg = format("Could not complete transaction '%s' within %s retries",
                    tx.getConfiguration().getFamilyName(), tx.getConfiguration().getMaxRetries());
            throw new TooManyRetriesException(msg);
        } finally {
            if (tx.getStatus() != TransactionStatus.Committed) {
                tx.abort();
            }
        }
    }

    public static AlphaTransaction handleSpeculativeFailure(TransactionFactory transactionFactory, AlphaTransaction oldTx) {
        oldTx.abort();
        AlphaTransaction newTx = (AlphaTransaction) transactionFactory.create();
        newTx.setRemainingTimeoutNs(oldTx.getRemainingTimeoutNs());
        newTx.setAttempt(oldTx.getAttempt());
        setThreadLocalTransaction(newTx);
        return newTx;
    }

    public static void handleRetry(AlphaTransaction tx) throws InterruptedException {
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
                    String msg = format("Transaction %s has timed out with a total timeout of %s ns",
                            tx.getConfiguration().getFamilyName(),
                            tx.getConfiguration().getTimeoutNs());
                    throw new RetryTimeoutException(msg);
                }
            }

            tx.reset();
        }
    }

    public static AlphaTransaction createTransaction(AlphaTransaction tx, TransactionFactory transactionFactory) {
        if (tx != null && tx.getTransactionFactory() == transactionFactory) {
            tx.reset();
            tx.setAttempt(0);
            tx.setRemainingTimeoutNs(tx.getConfiguration().getTimeoutNs());
        } else {
            tx = (AlphaTransaction) transactionFactory.create();
        }
        return tx;
    }

    // ===================== support methods ===========================

    public static boolean isActiveTransaction(AlphaTransaction t) {
        return t != null && !t.getStatus().isDead();
    }
}
