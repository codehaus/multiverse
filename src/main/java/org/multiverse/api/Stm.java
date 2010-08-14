package org.multiverse.api;

public interface Stm {

    /**
     * Gets the {@link TransactionFactoryBuilder} that needs to be used to execute transactions on this Stm. See the
     * {@link TransactionFactoryBuilder} for more info.
     *
     * @return the TransactionFactoryBuilder that needs to be used to execute transactions on this Stm.
     */
    TransactionFactoryBuilder getTransactionFactoryBuilder();
}
