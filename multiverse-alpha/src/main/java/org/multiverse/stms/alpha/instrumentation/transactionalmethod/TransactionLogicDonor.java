package org.multiverse.stms.alpha.instrumentation.transactionalmethod;

import org.multiverse.api.TransactionFactory;
import org.multiverse.api.TransactionStatus;
import org.multiverse.api.exceptions.*;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.utils.backoff.BackoffPolicy;
import org.multiverse.utils.latches.CheapLatch;
import org.multiverse.utils.latches.Latch;

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

    public static void execute() {
    }

    //fields can't be made final, the compiler could inline the values so can't be replaced by instrumentation.
    //By the instrumentation these static fields will be replaced by the actual TransactionalMethod parameters
    public static TransactionFactory transactionFactory;

    public static void donorConstructor() throws Exception {
        AlphaTransaction tx = (AlphaTransaction) getThreadLocalTransaction();

        if (isActiveTransaction(tx)) {
            execute();
            return;
        }

        tx = (AlphaTransaction) transactionFactory.start();
        setThreadLocalTransaction(tx);
        try {
            execute();
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

    public static void donorMethod() throws Exception {
        AlphaTransaction tx = (AlphaTransaction) getThreadLocalTransaction();

        if (isActiveTransaction(tx)) {
            execute();
            return;
        }

        tx = (AlphaTransaction) transactionFactory.start();
        setThreadLocalTransaction(tx);
        try {
            int attempt = 0;
            do {
                try {
                    attempt++;
                    execute();
                    tx.commit();
                    return;
                } catch (Retry er) {
                    if (attempt - 1 < tx.getConfiguration().getMaxRetryCount()) {
                        try {
                            waitForChange(tx);
                        } catch (SpeculativeConfigurationFailure e) {
                            tx = (AlphaTransaction) transactionFactory.start();
                            setThreadLocalTransaction(tx);
                        }
                    }
                } catch (SpeculativeConfigurationFailure tooSmallException) {
                    //todo
                    //tx.abort();
                    tx = (AlphaTransaction) transactionFactory.start();
                    setThreadLocalTransaction(tx);
                } catch (ControlFlowError throwable) {
                    System.out.println(throwable.getClass());
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

    public static void waitForChange(AlphaTransaction tx) throws InterruptedException {
        Latch latch = new CheapLatch();
        tx.registerRetryLatch(latch);
        if (tx.getConfiguration().isInterruptible()) {
            latch.await();
        } else {
            latch.awaitUninterruptible();
        }
    }

    public static boolean isActiveTransaction(AlphaTransaction t) {
        return t != null && t.getStatus() == TransactionStatus.active;
    }
}
