package org.multiverse.stms.beta;

import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.MonoBetaTransaction;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * @author Peter Veentjer
 */
public class StmUtils {

    public static void arbitraryUpdate(BetaStm stm, LongRef ref){
        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        tx.openForWrite(ref, false, new ObjectPool());
        tx.commit();
    }

    public static LongRef createLongRef(BetaStm stm) {
        return createLongRef(stm, 0);
    }

    public static LongRef createReadBiasedLongRef(BetaStm stm, long value) {
        LongRef ref = createLongRef(stm, value);

        for (int k = 0; k < ref.getOrec().getReadBiasedThreshold(); k++) {
            BetaTransaction tx = new MonoBetaTransaction(stm);
            tx.openForRead(ref, false, new ObjectPool());
            tx.commit(new ObjectPool());
        }

        return ref;
    }

    public static LongRef createReadBiasedLongRef(BetaStm stm) {
        return createReadBiasedLongRef(stm, 0);
    }

    public static LongRef createLongRef(BetaStm stm, long value) {
        LongRef ref = new LongRef(value);
        //BetaTransaction tx = new MonoBetaTransaction(stm);
        //ObjectPool pool = new ObjectPool();
        //LongRefTranlocal tranlocal = tx.openForConstruction(ref,  pool);
        //tranlocal.value = value;
        //tx.commit(pool);
        return ref;
    }

    public static String format(double value) {
        return NumberFormat.getInstance(Locale.ENGLISH).format(value);
    }

    public static String format(long value) {
        return NumberFormat.getInstance(Locale.ENGLISH).format(value);
    }
}
