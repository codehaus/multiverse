package org.multiverse.stms.alpha.instrumentation.asm;

import org.multiverse.api.*;
import org.multiverse.api.exceptions.RecoverableThrowable;
import org.multiverse.api.exceptions.RetryError;
import org.multiverse.api.exceptions.TooManyRetriesException;
import org.multiverse.utils.latches.CheapLatch;
import org.multiverse.utils.restartbackoff.RestartBackoffPolicy;

import static java.lang.String.format;
import static org.multiverse.api.ThreadLocalTransaction.*;

/**
 * The donor class that can be used while instrumenting atomic methods and adding the transaction management
 * donorMethod.
 * <p/>
 * It is important that if there are static methods used in this donor that they are public, so that they
 * still can be used if the donor methods are transplanted.
 *
 * @author Peter Veentjer.
 */
public class TransactionLogicDonor {

    public static void execute() {
    }

    //fields can't be made final, the compiler could inline the values so can't be replaced by instrumentation.
    //By the instrumentation these static fields will be replaced by the actual TransactionalMethod parameters
    public static TransactionFactory transactionFactory;

    public static void donorConstructor() throws Exception {
        Transaction tx = getThreadLocalTransaction();

        if (isActiveTransaction(tx)) {
            execute();
            return;
        }

        tx = transactionFactory.start();
        setThreadLocalTransaction(tx);
        try {
            execute();
            tx.commit();
            tx = null;
        } catch (Throwable throwable) {
            tx.abort();
            if (throwable instanceof RecoverableThrowable) {
                throw new TooManyRetriesException();
            } else if (throwable instanceof Error) {
                throw (Error) throwable;
            } else {
                throw (Exception) throwable;
            }
        } finally {
            clearThreadLocalTransaction();
        }
    }

    public static void donorMethod() throws Exception {
        Transaction tx = getThreadLocalTransaction();

        if (isActiveTransaction(tx)) {
            execute();
            return;
        }

        tx = transactionFactory.start();
        setThreadLocalTransaction(tx);
        try {
            int attempt = 0;
            do {
                try {
                    attempt++;
                    execute();
                    tx.commit();
                    return;
                } catch (RetryError er) {
                    waitForChange(tx);
                } catch (TransactionTooSmallException tooSmallException) {
                    tx = transactionFactory.start();
                    setThreadLocalTransaction(tx);
                } catch (Throwable throwable) {
                    if (throwable instanceof RecoverableThrowable) {
                        RestartBackoffPolicy backoffPolicy = tx.getConfig().getRestartBackoffPolicy();
                        backoffPolicy.backoffUninterruptible(tx, attempt);
                    } else if (throwable instanceof Exception) {
                        throw (Exception) throwable;
                    } else {
                        throw (Error) throwable;
                    }
                } finally {
                    if (tx.getStatus() != TransactionStatus.committed) {
                        if (attempt - 1 < tx.getConfig().getMaxRetryCount()) {
                            tx.restart();
                        } else {
                            tx.abort();
                        }
                    }
                }
            } while (attempt - 1 < tx.getConfig().getMaxRetryCount());

            String msg = format("Could not complete transaction '%s' within %s retries",
                    tx.getConfig().getFamilyName(), tx.getConfig().getMaxRetryCount());
            throw new TooManyRetriesException(msg);
        } finally {
            clearThreadLocalTransaction();
        }
    }

    public static void waitForChange(Transaction tx) throws InterruptedException {
        Latch latch = new CheapLatch();
        tx.registerRetryLatch(latch);
        if (tx.getConfig().isInterruptible()) {
            latch.await();
        } else {
            latch.awaitUninterruptible();
        }
    }

    public static void abortIfActive(Transaction t) {
        if (t.getStatus() == TransactionStatus.active) {
            t.abort();
        }
    }

    public static boolean isActiveTransaction(Transaction t) {
        return t != null && t.getStatus() == TransactionStatus.active;
    }
}
