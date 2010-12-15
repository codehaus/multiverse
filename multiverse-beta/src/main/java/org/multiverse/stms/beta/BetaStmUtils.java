package org.multiverse.stms.beta;

import org.multiverse.stms.beta.transactionalobjects.BetaTransactionalObject;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utility class for the BetaStm.
 *
 * @author Peter Veentjer
 */
public class BetaStmUtils implements BetaStmConstants {

    public static String toDebugString(BetaTransactionalObject o) {
        if (o == null) {
            return "null";
        } else {
            return o.getClass().getName() + "@" + System.identityHashCode(o);
        }
    }

    public static String format(double value) {
        return NumberFormat.getInstance(Locale.ENGLISH).format(value);
    }

    public static String format(long value) {
        return NumberFormat.getInstance(Locale.ENGLISH).format(value);
    }
}
