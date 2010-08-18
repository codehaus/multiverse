package org.multiverse.stms.beta;

import org.multiverse.stms.beta.refs.*;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.FatMonoBetaTransaction;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * @author Peter Veentjer
 */
public class BetaStmUtils {

    public static void arbitraryUpdate(BetaStm stm, LongRef ref) {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.openForWrite(ref, false, new BetaObjectPool());
        tx.commit();
    }

    public static <E> Ref<E> createRef(BetaStm stm, E value) {
        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        BetaObjectPool pool = new BetaObjectPool();
        Ref<E> ref = new Ref<E>(tx);
        RefTranlocal<E> tranlocal = tx.openForConstruction(ref, pool);
        tranlocal.value = value;
        tx.commit(pool);
        return ref;
    }

    public static Ref createRef(BetaStm stm) {
        return createRef(stm, null);
    }

    public static IntRef createIntRef(BetaStm stm) {
        return createIntRef(stm, 0);
    }

    public static IntRef createIntRef(BetaStm stm, int value) {
        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        BetaObjectPool pool = new BetaObjectPool();
        IntRef ref = new IntRef(tx);
        tx.openForConstruction(ref, pool).value = value;
        tx.commit(pool);
        return ref;
    }


    public static LongRef createLongRef(BetaStm stm) {
        return createLongRef(stm, 0);
    }

    public static LongRef createReadBiasedLongRef(BetaStm stm, long value) {
        LongRef ref = createLongRef(stm, value);

        for (int k = 0; k < ref.getOrec().getReadBiasedThreshold(); k++) {
            BetaTransaction tx = new FatMonoBetaTransaction(stm);
            tx.openForRead(ref, false, new BetaObjectPool());
            tx.commit(new BetaObjectPool());
        }

        return ref;
    }

    public static LongRef createReadBiasedLongRef(BetaStm stm) {
        return createReadBiasedLongRef(stm, 0);
    }

    public static LongRef createLongRef(BetaStm stm, long value) {
        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        BetaObjectPool pool = new BetaObjectPool();
        LongRef ref = new LongRef(tx);
        LongRefTranlocal tranlocal = tx.openForConstruction(ref, pool);
        tranlocal.value = value;
        tx.commit(pool);
        return ref;
    }

    public static String format(double value) {
        return NumberFormat.getInstance(Locale.ENGLISH).format(value);
    }

    public static String format(long value) {
        return NumberFormat.getInstance(Locale.ENGLISH).format(value);
    }
}
