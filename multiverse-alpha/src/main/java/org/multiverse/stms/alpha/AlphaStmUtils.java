package org.multiverse.stms.alpha;

import static org.multiverse.api.ThreadLocalTransaction.getRequiredThreadLocalTransaction;

import static java.lang.String.format;

/**
 * Utility class for various utility methods.
 *
 * @author Peter Veentjer
 */
public final class AlphaStmUtils {

    public static String getLoadUncommittedMessage(AlphaAtomicObject atomicObject) {
        return format("Load uncommitted on atomicobject '%s' ", toAtomicObjectString(atomicObject));
    }

    /**
     * Debug string representation of the atomicobject that belongs to the tranlocal.
     *
     * @param tranlocal
     * @return the string representation of the atomicobject belonging to the tranlocal.
     */
    public static String toAtomicObjectString(AlphaTranlocal tranlocal) {
        return toAtomicObjectString(tranlocal.getAtomicObject() == null ? null : tranlocal.getAtomicObject());
    }


    /**
     * Debug representation of the atomicobject.
     *
     * @param atomicObject
     * @return the string representation of the atomicobject.
     */
    public static String toAtomicObjectString(AlphaAtomicObject atomicObject) {
        if (atomicObject == null) {
            return "null";
        }
        return format("%s@%s", atomicObject.getClass().getName(), System.identityHashCode(atomicObject));
    }

    /**
     * Gets the AtomicObject for the provided AlphaTranlocal.
     *
     * @param tranlocal the AlphaTranlocal.
     * @return the AlphaAtomicObject that belongs to the tranlocal, or null if tranlocal is null.
     */
    public static AlphaAtomicObject getAtomicObject(AlphaTranlocal tranlocal) {
        return tranlocal == null ? null : tranlocal.getAtomicObject();
    }

    /**
     * Loads a Tranlocal using a transaction. The transaction is retrieved from the
     * ThreadLocalTransaction. If no transaction is found, a RuntimeException is thrown.
     * <p/>
     * For more information see {@link AlphaTransaction#load(AlphaAtomicObject)}
     * <p/>
     * This method is called by instrumented atomicobjects.
     *
     * @param atomicObject the AlphaAtomicObject.
     * @return the AlphaTranlocal
     */
    public static AlphaTranlocal load(Object atomicObject) {
        AlphaTransaction t = (AlphaTransaction) getRequiredThreadLocalTransaction();
        return t.load((AlphaAtomicObject) atomicObject);
    }

    //we don't want instances
    private AlphaStmUtils() {
    }
}
