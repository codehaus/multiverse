package org.multiverse.stms.beta;

import org.multiverse.stms.beta.transactionalobjects.*;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.FatMonoBetaTransaction;
import org.multiverse.stms.beta.transactions.LeanMonoBetaTransaction;

import java.text.NumberFormat;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

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
            return o.getClass().getName()+"@"+System.identityHashCode(o);
        }
    }

    public static String format(double value) {
        return NumberFormat.getInstance(Locale.ENGLISH).format(value);
    }

    public static String format(long value) {
        return NumberFormat.getInstance(Locale.ENGLISH).format(value);
    }
}
