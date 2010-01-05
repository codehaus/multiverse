package org.multiverse.api;

/**
 * @author Peter Veentjer
 */
public class Transactions {

    public static Transaction startUpdateTransaction(Stm stm) {
        return stm.getTransactionFactoryBuilder()
                .setReadonly(false)
                .build()
                .start();
    }

    public static Transaction startReadonlyTransaction(Stm stm) {
        return stm.getTransactionFactoryBuilder()
                .setReadonly(true)
                .build()
                .start();
    }


    private Transactions(){}
}
