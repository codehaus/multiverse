package org.multiverse.stms.alpha;

import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static java.lang.String.format;
import static org.multiverse.api.ThreadLocalTransaction.getRequiredThreadLocalTransaction;

/**
 * Utility class for various utility methods.
 *
 * @author Peter Veentjer
 */
public final class AlphaStmUtils {

    public static String getLoadUncommittedMessage(AlphaTransactionalObject object) {
        return format("Load uncommitted on transactionalobject '%s' ", toTxObjectString(object));
    }

    /**
     * Debug string representation of the atomicobject that belongs to the tranlocal.
     *
     * @param tranlocal
     * @return the string representation of the atomicobject belonging to the tranlocal.
     */
    public static String toTransactionalObjectString(AlphaTranlocal tranlocal) {
        return toTxObjectString(tranlocal.getTransactionalObject() == null ? null : tranlocal.getTransactionalObject());
    }


    /**
     * Debug representation of a TransactionalObject.
     *
     * @param object
     * @return the string representation of the atomicobject.
     */
    public static String toTxObjectString(AlphaTransactionalObject object) {
        if (object == null) {
            return "null";
        }
        return format("%s@%s", object.getClass().getName(), System.identityHashCode(object));
    }


   
    /**
     * Gets the TransactionalObject for the provided AlphaTranlocal.
     *
     * @param tranlocal the AlphaTranlocal.
     * @return the AlphaTransactionalObject that belongs to the tranlocal, or null if tranlocal is null.
     */
    public static AlphaTransactionalObject getTransactionalObject(AlphaTranlocal tranlocal) {
        return tranlocal == null ? null : tranlocal.getTransactionalObject();
    }

    //we don't want instances

    private AlphaStmUtils() {
    }
}
