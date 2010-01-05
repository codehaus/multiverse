package org.multiverse.api;

import org.multiverse.api.exceptions.NoTransactionFoundException;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A {@link ThreadLocal} that contains the current {@link Transaction}.
 *
 * @author Peter Veentjer.
 */
public final class ThreadLocalTransaction {

    public final static AtomicLong getCount = new AtomicLong();
    public final static AtomicLong setCount = new AtomicLong();

    public final static boolean STATISTICS_ENABLED = false;

    public final static ThreadLocal<Transaction> threadlocal = new ThreadLocal<Transaction>();

    public static void clearThreadLocalTransaction() {
        if (STATISTICS_ENABLED) {
            setCount.incrementAndGet();
        }

        threadlocal.set(null);
    }

    public static Transaction getThreadLocalTransaction() {
        if (STATISTICS_ENABLED) {
            getCount.incrementAndGet();
        }

        return threadlocal.get();
    }

    public static Transaction getRequiredThreadLocalTransaction() {
        if (STATISTICS_ENABLED) {
            getCount.incrementAndGet();
        }

        Transaction t = threadlocal.get();
        if (t == null) {
            throw new NoTransactionFoundException("No transaction is found on the ThreadLocalTransaction");
        }

        return t;
    }


    public static void setThreadLocalTransaction(Transaction newTransaction) {
        if (STATISTICS_ENABLED) {
            setCount.incrementAndGet();
        }

        threadlocal.set(newTransaction);
    }

    //we don't want any instances.
    private ThreadLocalTransaction() {
    }
}
