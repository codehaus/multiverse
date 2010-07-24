package org.multiverse.stms.beta;

import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.TransactionFactoryBuilder;
import org.multiverse.durability.*;
import org.multiverse.stms.beta.conflictcounters.GlobalConflictCounter;
import org.multiverse.stms.beta.transactions.ArrayTreeBetaTransaction;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.BetaTransactionConfig;

/**
 * @author Peter Veentjer
 */
public final class BetaStm {

    private final GlobalConflictCounter globalConflictCounter;
    private final int spinCount = 8;
    private final BetaTransactionConfig config;
    private final SimpleStorage storage;

    public BetaStm() {
        this(1);
    }

    public BetaStm(int conflictCounterWidth) {
        this.globalConflictCounter = new GlobalConflictCounter(conflictCounterWidth);
        this.config = new BetaTransactionConfig(this)
                .setSpinCount(spinCount);
        this.storage = new SimpleStorage();
        initStorage();
    }

    private void initStorage() {
/*
        this.storage.register(IntRef.class, new DurableObjectSerializer() {
            @Override
            public byte[] getBytes(State state) {
                IntRefTranlocal tranlocal = (IntRefTranlocal) state;
                return Integer.toString(tranlocal.value).getBytes();
            }

            @Override
            public DurableObject deserializeObject(String id, byte[] content) {
                int value = Integer.parseInt(new String(content));
                IntRef ref = new IntRef(value);
                ref.setStorageId(id);
                return ref;
            }
        });

        this.storage.register(LongRef.class, new DurableObjectSerializer() {
            @Override
            public byte[] getBytes(State state) {
                LongRefTranlocal tranlocal = (LongRefTranlocal) state;
                return Long.toString(tranlocal.value).getBytes();
            }

            @Override
            public DurableObject deserializeObject(String id, byte[] content) {
                long value = Long.parseLong(new String(content));
                LongRef ref = new LongRef(value);
                ref.setStorageId(id);
                return ref;
            }
        });

        this.storage.register(DoubleRef.class, new DurableObjectSerializer() {
            @Override
            public byte[] getBytes(State state) {
                DoubleRefTranlocal tranlocal = (DoubleRefTranlocal) state;
                return Double.toString(tranlocal.value).getBytes();
            }

            @Override
            public DurableObject deserializeObject(String id, byte[] content) {
                double value = Double.parseDouble(new String(content));
                DoubleRef ref = new DoubleRef(value);
                ref.setStorageId(id);
                return ref;
            }
        });*/
    }

    public Storage getStorage() {
        return storage;
    }

    public TransactionFactoryBuilder createTransactionFactoryBuilder() {
        return new TransactionFactoryBuilderImpl(config);
    }

    public int getSpinCount() {
        return spinCount;
    }

    public GlobalConflictCounter getGlobalConflictCounter() {
        return globalConflictCounter;
    }

    public BetaTransaction start() {
        return new ArrayTreeBetaTransaction(config);
    }

    class TransactionFactoryBuilderImpl implements TransactionFactoryBuilder {

        private final BetaTransactionConfig config;

        TransactionFactoryBuilderImpl(BetaTransactionConfig config) {
            this.config = config;
        }

        @Override
        public TransactionFactoryBuilder setPessimisticLockLevel(PessimisticLockLevel lockLevel) {
            return new TransactionFactoryBuilderImpl(config.setPessimisticLockLevel(lockLevel));
        }

        @Override
        public TransactionFactory build() {
            return new TransactionFactoryImpl(config);
        }

        @Override
        public TransactionFactoryBuilder setDirtyCheckEnabled(boolean dirtyCheckEnabled) {
            return new TransactionFactoryBuilderImpl(config.setDirtyCheckEnabled(dirtyCheckEnabled));
        }

        @Override
        public TransactionFactoryBuilder setSpinCount(int spinCount) {
            return new TransactionFactoryBuilderImpl(config.setSpinCount(spinCount));
        }

        @Override
        public TransactionFactoryBuilder setReadonly(boolean readonly) {
            return new TransactionFactoryBuilderImpl(config.setReadonly(readonly));
        }
    }

    class TransactionFactoryImpl implements TransactionFactory {

        private BetaTransactionConfig config;

        TransactionFactoryImpl(BetaTransactionConfig config) {
            this.config = config;
        }

        @Override
        public BetaTransaction start() {
            return new ArrayTreeBetaTransaction(config);
        }
    }
}
