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

        tx = (AlphaTransaction) transactionFactory.start();
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

    public static int donorGetter() {
        return 0;
    }

    public static void donorSetter(int value) {

    }

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

        tx = (AlphaTransaction) transactionFactory.start();
        setThreadLocalTransaction(tx);
        try {
            int attempt = 0;
            do {
                try {
                    try {
                        attempt++;
                        if (tx.getConfiguration().isReadonly()) {
                            execute___ro();
                        } else {
                            execute___up();
                        }

                        tx.commit();
                        return;
                    } catch (Retry er) {
                        if (attempt - 1 < tx.getConfiguration().getMaxRetryCount()) {
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

                                long durationNs = beginNs - System.nanoTime();
                                tx.setRemainingTimeoutNs(tx.getRemainingTimeoutNs() - durationNs);

                                if (timeout) {
                                    String msg = format("Transaction %s has timed with a total timeout of %s ns",
                                            tx.getConfiguration().getFamilyName(),
                                            tx.getConfiguration().getTimeoutNs());
                                    throw new RetryTimeoutException(msg);
                                }
                            }

                            tx.restart();
                        }
                    }
                } catch (SpeculativeConfigurationFailure tooSmallException) {
                    AlphaTransaction oldTx = tx;
                    tx = (AlphaTransaction) transactionFactory.start();
                    tx.setRemainingTimeoutNs(oldTx.getRemainingTimeoutNs());
                    setThreadLocalTransaction(tx);
                } catch (ControlFlowError throwable) {
                    BackoffPolicy backoffPolicy = tx.getConfiguration().getBackoffPolicy();
                    backoffPolicy.delayedUninterruptible(tx, attempt);
                } finally {
                    if (tx.getStatus() != TransactionStatus.committed && attempt - 1 < tx.getConfiguration().getMaxRetryCount()) {
                        tx.restart();
                    }
                }
            } while (attempt - 1 < tx.getConfiguration().getMaxRetryCount());

            String msg = format("Could not complete transaction '%s' within %s retries",
                    tx.getConfiguration().getFamilyName(), tx.getConfiguration().getMaxRetryCount());
            throw new TooManyRetriesException(msg);
        } finally {
            clearThreadLocalTransaction();

            if (tx.getStatus() != TransactionStatus.committed) {
                tx.abort();
            }
        }
    }

    // ===================== support methods ===========================

    public static boolean isActiveTransaction(AlphaTransaction t) {
        return t != null && t.getStatus() == TransactionStatus.active;
    }
}
