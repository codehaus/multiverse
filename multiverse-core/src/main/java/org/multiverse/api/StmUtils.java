package org.multiverse.api;

import org.multiverse.api.exceptions.NoTransactionFoundException;
import org.multiverse.api.exceptions.RetryError;

import static org.multiverse.api.ThreadLocalTransaction.getRequiredThreadLocalTransaction;

/**
 * A utility class with convenience methods to access the {@link org.multiverse.api.Stm} or
 * {@link Transaction}. These methods can be imported using the static import for a less
 * ugly (but potentially more confusing) syntax.
 *
 * @author Peter Veentjer.
 */
public final class StmUtils {

    /**
     * Does a retry.
     * <p/>
     * Under the hood the retry throws an RetryError that will be caught up the chain
     * (by the TransactionTemplate for example).
     *
     * @throws NoTransactionFoundException if no transaction is found in the ThreadLocalTransaction.
     */
    public static void retry() {
        getRequiredThreadLocalTransaction();
        throw RetryError.create();
    }

    //we don't want instances.

    private StmUtils() {
    }
}
