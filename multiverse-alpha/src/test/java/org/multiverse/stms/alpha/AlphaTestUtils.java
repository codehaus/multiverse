package org.multiverse.stms.alpha;

import org.multiverse.stms.alpha.transactions.AlphaTransaction;

public class AlphaTestUtils {

    public static AlphaTransaction startTrackingUpdateTransaction(AlphaStm stm) {
        return stm.getTransactionFactoryBuilder()
                .setReadonly(false)
                .setAutomaticReadTrackingEnabled(true)
                .build()
                .start();
    }


}
