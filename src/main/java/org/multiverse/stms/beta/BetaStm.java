package org.multiverse.stms.beta;

import org.multiverse.api.*;
import org.multiverse.durability.SimpleStorage;
import org.multiverse.durability.Storage;
import org.multiverse.stms.beta.conflictcounters.GlobalConflictCounter;
import org.multiverse.stms.beta.transactions.*;

import java.util.concurrent.ConcurrentHashMap;

import static org.multiverse.stms.beta.ThreadLocalBetaObjectPool.getThreadLocalBetaObjectPool;

/**
 * All non anonymous transactionfactories (so transactions with a familyname set explicitly) will
 * be stored in the factoryMap. If a transactionfactory with the same name already is stored in the
 * factoryMap, it will not be stored.
 *
 * @author Peter Veentjer
 */
public final class BetaStm implements Stm {

    private final AtomicBlock atomicBlock;
    private final GlobalConflictCounter globalConflictCounter;
    private final int spinCount = 8;
    private final BetaTransactionConfig config;
    private final SimpleStorage storage;

    private final ConcurrentHashMap<String, BetaTransactionFactory> factoryMap
            = new ConcurrentHashMap<String, BetaTransactionFactory>();

    public BetaStm() {
        this(1);
    }

    public BetaStm(final int conflictCounterWidth) {
        this.globalConflictCounter = new GlobalConflictCounter(conflictCounterWidth);
        this.config = new BetaTransactionConfig(this)
                .setSpinCount(spinCount);
        this.storage = new SimpleStorage(this);
        this.atomicBlock = getTransactionFactoryBuilder()
                .setSpeculativeConfigEnabled(false)
                .buildAtomicBlock();
    }

    public Storage getStorage() {
        return storage;
    }

    public BetaTransactionFactoryBuilder getTransactionFactoryBuilder() {
        return new BetaTransactionFactoryBuilderImpl(config);
    }

    public int getSpinCount() {
        return spinCount;
    }

    public GlobalConflictCounter getGlobalConflictCounter() {
        return globalConflictCounter;
    }

    public BetaTransaction start() {
        return new FatArrayTreeBetaTransaction(config);
    }

    public AtomicBlock getDefaultAtomicBlock(){
        return atomicBlock;
    }

    public int getMaxArrayTransactionSize() {
        return 20;
    }

    public final class BetaTransactionFactoryBuilderImpl implements BetaTransactionFactoryBuilder {

        private final BetaTransactionConfig config;

        BetaTransactionFactoryBuilderImpl(final BetaTransactionConfig config) {
            this.config = config;
        }

        @Override
        public BetaTransactionFactoryBuilder setPropagationLevel(final PropagationLevel level) {
            return new BetaTransactionFactoryBuilderImpl(config.setPropagationLevel(level));
        }

        @Override
        public BetaTransactionFactoryBuilder setBlockingAllowed(final boolean blockingAllowed) {
            return new BetaTransactionFactoryBuilderImpl(config.setBlockingAllowed(blockingAllowed));
        }

        @Override
        public BetaTransactionFactoryBuilder setWriteSkewAllowed(final boolean writeSkewAllowed) {
            return new BetaTransactionFactoryBuilderImpl(config.setWriteSkewAllowed(writeSkewAllowed));
        }

        @Override
        public BetaTransactionFactoryBuilder setTraceLevel(final TraceLevel traceLevel) {
            return new BetaTransactionFactoryBuilderImpl(config.setTraceLevel(traceLevel));
        }

        @Override
        public BetaTransactionFactoryBuilder setTimeoutNs(final long timeoutNs) {
            return new BetaTransactionFactoryBuilderImpl(config.setTimeoutNs(timeoutNs));
        }

        @Override
        public BetaTransactionFactoryBuilder setInterruptible(final boolean interruptible) {
            return new BetaTransactionFactoryBuilderImpl(config.setInterruptible(interruptible));
        }

        @Override
        public BetaTransactionFactoryBuilder setBackoffPolicy(final BackoffPolicy backoffPolicy) {
            return new BetaTransactionFactoryBuilderImpl(config.setBackoffPolicy(backoffPolicy));
        }

        @Override
        public BetaTransactionFactoryBuilder setPessimisticLockLevel(final PessimisticLockLevel lockLevel) {
            return new BetaTransactionFactoryBuilderImpl(config.setPessimisticLockLevel(lockLevel));
        }

        @Override
        public BetaTransactionFactoryBuilder setDirtyCheckEnabled(final boolean dirtyCheckEnabled) {
            return new BetaTransactionFactoryBuilderImpl(config.setDirtyCheckEnabled(dirtyCheckEnabled));
        }

        @Override
        public BetaTransactionFactoryBuilder setSpinCount(final int spinCount) {
            return new BetaTransactionFactoryBuilderImpl(config.setSpinCount(spinCount));
        }

        @Override
        public BetaTransactionFactoryBuilder setSpeculativeConfigEnabled(final boolean speculativeConfigEnabled) {
            return new BetaTransactionFactoryBuilderImpl(
                    config.setSpeculativeConfigurationEnabled(speculativeConfigEnabled));
        }

        @Override
        public BetaTransactionFactoryBuilder setReadonly(final boolean readonly) {
            return new BetaTransactionFactoryBuilderImpl(config.setReadonly(readonly));
        }

        @Override
        public BetaTransactionFactoryBuilder setReadTrackingEnabled(final boolean enabled) {
            return new BetaTransactionFactoryBuilderImpl(config.setReadTrackingEnabled(enabled));
        }

        @Override
        public BetaTransactionFactoryBuilder setMaxRetries(final int maxRetries) {
            return new BetaTransactionFactoryBuilderImpl(config.setMaxRetries(maxRetries));
        }

        @Override
        public AtomicBlock buildAtomicBlock() {
            if(leanAtomicBlock()){
                return new LeanBetaAtomicBlock(build());
            }else{
                return new FatBetaAtomicBlock(build());
            }
        }

        private boolean leanAtomicBlock() {
            return config.propagationLevel == PropagationLevel.Requires;
        }

        @Override
        public BetaTransactionFactory build() {
            config.validate();

            BetaTransactionFactory factory;
            if (config.isSpeculativeConfigEnabled()) {
                factory = new SpeculativeBetaTransactionFactory(config);
            } else {
                factory = new NonSpeculativeBetaTransactionFactory(config);
            }

            if (!config.isAnonymous) {
                factoryMap.putIfAbsent(config.familyName, factory);
            }

            return factory;
        }
    }

    public final class NonSpeculativeBetaTransactionFactory implements BetaTransactionFactory {

        private final BetaTransactionConfig config;

        NonSpeculativeBetaTransactionFactory(final BetaTransactionConfig config) {
            this.config = config;
        }

        @Override
        public BetaTransactionConfig getTransactionConfiguration() {
            return config;
        }

        @Override
        public BetaTransaction start() {
            return start(getThreadLocalBetaObjectPool());
        }

        @Override
        public BetaTransaction start(final BetaObjectPool pool) {
            FatArrayTreeBetaTransaction tx = pool.takeFatArrayTreeBetaTransaction();

            if (tx == null) {
                tx = new FatArrayTreeBetaTransaction(config);
            } else {
                tx.init(config, pool);
            }

            return tx;
        }

        @Override
        public BetaTransaction upgradeAfterSpeculativeFailure(
                final BetaTransaction failingTransaction, final BetaObjectPool pool) {
            throw new UnsupportedOperationException();
        }
    }

    public final class SpeculativeBetaTransactionFactory implements BetaTransactionFactory {

        private final BetaTransactionConfig config;

        SpeculativeBetaTransactionFactory(final BetaTransactionConfig config) {
            this.config = config;
        }

        @Override
        public BetaTransactionConfig getTransactionConfiguration() {
            return config;
        }

        @Override
        public BetaTransaction start() {
            return start(getThreadLocalBetaObjectPool());
        }

        @Override
        public BetaTransaction upgradeAfterSpeculativeFailure(
                final BetaTransaction failingTransaction, final BetaObjectPool pool) {
            final BetaTransaction tx = start(pool);
            tx.copyForSpeculativeFailure(failingTransaction);
            return tx;
        }

        @Override
        public BetaTransaction start(final BetaObjectPool pool) {
            final SpeculativeBetaConfig speculativeConfig = config.getSpeculativeConfig();
            final int length = speculativeConfig.getMinimalLength();

            if (length <= 1) {
                if (speculativeConfig.isListenerRequired()) {
                    FatMonoBetaTransaction tx = pool.takeFatMonoBetaTransaction();
                    if (tx == null) {
                        tx = new FatMonoBetaTransaction(config);
                    } else {
                        tx.init(config, pool);
                    }
                    return tx;
                } else {
                    LeanMonoBetaTransaction tx = pool.takeLeanMonoBetaTransaction();
                    if (tx == null) {
                        tx = new LeanMonoBetaTransaction(config);
                    } else {
                        tx.init(config, pool);
                    }
                    return tx;
                }
            } else if (length <= config.getMaxArrayTransactionSize()) {
                if (speculativeConfig.isListenerRequired()) {
                    FatArrayBetaTransaction tx = pool.takeFatArrayBetaTransaction();
                    if (tx == null) {
                        tx = new FatArrayBetaTransaction(config);
                    } else {
                        tx.init(config, pool);
                    }
                    return tx;
                } else {
                    LeanArrayBetaTransaction tx = pool.takeLeanArrayBetaTransaction();
                    if (tx == null) {
                        tx = new LeanArrayBetaTransaction(config);
                    } else {
                        tx.init(config, pool);
                    }
                    return tx;
                }
            } else {
                if (speculativeConfig.isListenerRequired()) {
                    FatArrayTreeBetaTransaction tx = pool.takeFatArrayTreeBetaTransaction();
                    if (tx == null) {
                        tx = new FatArrayTreeBetaTransaction(config);
                    } else {
                        tx.init(config, pool);
                    }
                    return tx;
                } else {
                    LeanArrayTreeBetaTransaction tx = pool.takeLeanArrayTreeBetaTransaction();
                    if (tx == null) {
                        tx = new LeanArrayTreeBetaTransaction(config);
                    } else {
                        tx.init(config, pool);
                    }
                    return tx;
                }
            }
        }
    }
}
