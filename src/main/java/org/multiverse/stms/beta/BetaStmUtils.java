package org.multiverse.stms.beta;

import org.multiverse.stms.beta.transactionalobjects.*;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.FatMonoBetaTransaction;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * @author Peter Veentjer
 */
public class BetaStmUtils {

    public static String toDebugString(BetaTransactionalObject o){
        if(o == null){
            return "null";
        }else{
            return o.getClass().getName();
        }
    }

    public static void arbitraryUpdate(BetaStm stm, BetaLongRef ref) {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.openForWrite(ref, false);
        tx.commit();
    }

    public static <E> BetaRef<E> createRef(BetaStm stm, E value) {
        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        BetaRef<E> ref = new BetaRef<E>(tx);
        RefTranlocal<E> tranlocal = tx.openForConstruction(ref);
        tranlocal.value = value;
        tx.commit();
        return ref;
    }

    public static BetaRef createRef(BetaStm stm) {
        return createRef(stm, null);
    }

    public static BetaIntRef createIntRef(BetaStm stm) {
        return createIntRef(stm, 0);
    }

    public static BetaIntRef createIntRef(BetaStm stm, int value) {
        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        BetaIntRef ref = new BetaIntRef(tx);
        tx.openForConstruction(ref).value = value;
        tx.commit();
        return ref;
    }


    public static BetaLongRef createLongRef(BetaStm stm) {
        return createLongRef(stm, 0);
    }

    public static BetaLongRef createReadBiasedLongRef(BetaStm stm, long value) {
        BetaLongRef ref = createLongRef(stm, value);

        for (int k = 0; k < ref.___getOrec().___getReadBiasedThreshold(); k++) {
            BetaTransaction tx = new FatMonoBetaTransaction(stm);
            tx.openForRead(ref, false);
            tx.commit();
        }

        return ref;
    }

    public static BetaLongRef createReadBiasedLongRef(BetaStm stm) {
        return createReadBiasedLongRef(stm, 0);
    }

    public static BetaLongRef createLongRef(BetaStm stm, long value) {
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
