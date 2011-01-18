package org.multiverse.stms.gamma;

import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.TransactionRequiredException;
import org.multiverse.stms.gamma.transactionalobjects.GammaObject;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static java.lang.String.format;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public final class GammaStmUtils {

    public static String toDebugString(GammaObject o) {
        if (o == null) {
            return "null";
        } else {
            return o.getClass().getName() + '@' + System.identityHashCode(o);
        }
    }

    public static GammaTransaction getRequiredThreadLocalGammaTransaction() {
        final Transaction tx = getThreadLocalTransaction();

        if (tx == null) {
            throw new TransactionRequiredException();
        }

        return asGammaTransaction(tx);
    }

    public static GammaTransaction asGammaTransaction(final Transaction tx) {
        if (tx instanceof GammaTransaction) {
            return (GammaTransaction) tx;
        }

        if (tx == null) {
            throw new NullPointerException("Transaction can't be null");
        }

        tx.abort();
        throw new ClassCastException(
                format("Expected Transaction of class %s, found %s", GammaTransaction.class.getName(), tx.getClass().getName()));
    }

    //we don't want instances.
    private GammaStmUtils() {
    }
}
