package org.multiverse.api;

/**
 * @author Peter Veentjer
 */
public interface TransactionFactoryBuilder {

    TransactionFactoryBuilder setPessimisticLockLevel(PessimisticLockLevel lockLevel);

    TransactionFactoryBuilder setDirtyCheckEnabled(boolean dirtyCheckEnabled);

    TransactionFactoryBuilder setSpinCount(int spinCount);

    TransactionFactoryBuilder setReadonly(boolean readonly);

    TransactionFactory build();
}
