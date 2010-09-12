package org.multiverse.stms.beta;

import org.multiverse.stms.beta.transactionalobjects.*;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.FatMonoBetaTransaction;
import org.multiverse.stms.beta.transactions.LeanMonoBetaTransaction;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * @author Peter Veentjer
 */
public class BetaStmUtils {

    public static String toDebugString(BetaTransactionalObject o) {
        if (o == null) {
            return "null";
        } else {
            return o.getClass().getName();
        }
    }

    public static void arbitraryUpdate(BetaStm stm, BetaLongRef ref) {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.openForWrite(ref, false);
        tx.commit();
    }

    public static <E> BetaRef<E> newRef(BetaStm stm, E value) {
        BetaTransaction tx = new LeanMonoBetaTransaction(stm);
        BetaRef<E> ref = new BetaRef<E>(tx);
        RefTranlocal<E> tranlocal = tx.openForConstruction(ref);
        tranlocal.value = value;
        tx.commit();
        return ref;
    }

    public static BetaDoubleRef newDoubleRef(BetaStm stm, double value) {
        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        BetaDoubleRef ref = new BetaDoubleRef(tx);
        DoubleRefTranlocal tranlocal = tx.openForConstruction(ref);
        tranlocal.value = value;
        tx.commit();
        return ref;
    }


    public static BetaRef newRef(BetaStm stm) {
        return newRef(stm, null);
    }

     public static BetaIntRef newIntRef(BetaStm stm) {
        return newIntRef(stm, 0);
    }

    public static BetaIntRef newIntRef(BetaStm stm, int value) {
        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        BetaIntRef ref = new BetaIntRef(tx);
        tx.openForConstruction(ref).value = value;
        tx.commit();
        return ref;
    }


    public static BetaLongRef newLongRef(BetaStm stm) {
        return newLongRef(stm, 0);
    }

    public static BetaLongRef newReadBiasedLongRef(BetaStm stm, long value) {
        BetaLongRef ref = newLongRef(stm, value);

        for (int k = 0; k < ref.___getOrec().___getReadBiasedThreshold(); k++) {
            BetaTransaction tx = new FatMonoBetaTransaction(stm);
            tx.openForRead(ref, false);
            tx.commit();
        }

        return ref;
    }

    public static BetaLongRef newReadBiasedLongRef(BetaStm stm) {
        return newReadBiasedLongRef(stm, 0);
    }

    public static BetaLongRef newLongRef(BetaStm stm, long value) {
        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        BetaLongRef ref = new BetaLongRef(tx);
        LongRefTranlocal tranlocal = tx.openForConstruction(ref);
        tranlocal.value = value;
        tx.commit();
        return ref;
    }

   
    public static String format(double value) {
        return NumberFormat.getInstance(Locale.ENGLISH).format(value);
    }

    public static String format(long value) {
        return NumberFormat.getInstance(Locale.ENGLISH).format(value);
    }
}
