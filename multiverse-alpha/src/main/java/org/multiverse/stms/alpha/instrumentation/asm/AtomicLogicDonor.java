package org.multiverse.stms.alpha.instrumentation.asm;

import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.*;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionStatus;
import org.multiverse.api.exceptions.RecoverableThrowable;
import org.multiverse.api.exceptions.RetryError;
import org.multiverse.api.exceptions.TooManyRetriesException;
import org.multiverse.templates.AbortedException;
import org.multiverse.utils.latches.CheapLatch;
import org.multiverse.utils.latches.Latch;

import static java.lang.String.format;

/**
 * The donor class that can be used while instrumenting atomic methods and adding the transaction management
 * donorMethod.
 * <p/>
 * Donor method must be static Donor method must have 3 arguments: readonly:boolean, familyName:string, retryCount:int
 * Donor method must be called 'donorMethod' Donor method must return void. Donor method may not have argument arguments
 * called arg$ with $ being some number.
 *
 * @author Peter Veentjer.
 */
public class AtomicLogicDonor {

    public static void execute() {
    }

    //fields can't be made final, the compiler could inline the values so can't be replaced by instrumentation.
    //By the instrumentation these static fields will be replaced by the actual atomicmethod parameters
    //It isn't allowed to do writes.
    public static boolean readOnly = false;
    public static String familyName = null;
    public static int retryCount = 10;

    public static void donorConstructor() throws Exception {
        Transaction t = getThreadLocalTransaction();

        if (isActiveTransaction(t)) {
            execute();
        } else {
            t = createTransaction(readOnly, familyName);
            setThreadLocalTransaction(t);
            try {
                execute();
                if (t.getStatus() == TransactionStatus.aborted) {
                    throw new AbortedException();
                }
                t.commit();
                t = null;
            } catch (Throwable throwable) {
                t.abort();
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
    }

    public static void donorMethod() throws Exception {
        Transaction t = getThreadLocalTransaction();

        if (isActiveTransaction(t)) {
            execute();
            return;
        }

        t = createTransaction(readOnly, familyName);

        int attempt = 1;
        do {
            setThreadLocalTransaction(t);
            try {
                execute();

                if (t.getStatus() == TransactionStatus.aborted) {
                    throw new AbortedException();
                }
                t.commit();
                return;
            } catch (Throwable throwable) {
                if (throwable instanceof RetryError) {
                    Latch latch = new CheapLatch();
                    t.abortAndRegisterRetryLatch(latch);
                    latch.awaitUninterruptible();
                } else if (throwable instanceof RecoverableThrowable) {
                    t.getRestartBackoffPolicy().delayUninterruptible(t, attempt);
                    //backoff(attempt);
                    //ignore
                } else if (throwable instanceof Exception) {
                    throw (Exception) throwable;
                } else {
                    throw (Error) throwable;
                }
            } finally {
                abortIfActive(t);
                clearThreadLocalTransaction();
            }

            if (attempt - 1 == retryCount) {
                t = null;
            } else {
                t = t.abortAndReturnRestarted();
                attempt++;
            }
        } while (t != null);

        String msg = format("Could not complete transaction '%s' within %s retries", familyName, retryCount);
        throw new TooManyRetriesException(msg);
    }

    public static Transaction createTransaction(boolean readonly, String familyName) {
        Transaction t;
        if (readonly) {
            t = getGlobalStmInstance().startReadOnlyTransaction(familyName);
        } else {
            t = getGlobalStmInstance().startUpdateTransaction(familyName);
        }
        return t;
    }

    public static void abortIfActive(final Transaction t) {
        if (t.getStatus() == TransactionStatus.active) {
            t.abort();
        }
    }

    public static boolean isActiveTransaction(final Transaction t) {
        return t != null && t.getStatus() == TransactionStatus.active;
    }
}
