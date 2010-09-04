package org.multiverse.stms.beta;

import org.multiverse.api.*;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.api.references.ReferenceFactoryBuilder;
import org.multiverse.durability.SimpleStorage;
import org.multiverse.durability.Storage;
import org.multiverse.sensors.SimpleProfiler;
import org.multiverse.stms.beta.conflictcounters.GlobalConflictCounter;
import org.multiverse.stms.beta.transactionalobjects.BetaIntRef;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.BetaRef;
import org.multiverse.stms.beta.transactions.*;

import static org.multiverse.stms.beta.ThreadLocalBetaTransactionPool.getThreadLocalBetaTransactionPool;

/**
 * All non anonymous transactionfactories (so transactions with a familyname getAndSet explicitly) will
 * be stored in the factoryMap. If a transactionfactory with the same name already is stored in the
 * factoryMap, it will not be stored.
 *
 * @author Peter Veentjer
 */
public final class BetaStm implements Stm {

    private final AtomicBlock atomicBlock;
    private final GlobalConflictCounter globalConflictCounter;
    private final int spinCount = 8;
    private final BetaTransactionConfiguration defaultConfig;
    private final SimpleStorage storage;
    private final SimpleProfiler simpleProfiler = new SimpleProfiler();
    private final StmCallback callback;

    public BetaStm() {
        this(1, null);
    }

    public BetaStm(final int conflictCounterWidth, StmCallback callback) {
        this.callback = callback;
        this.globalConflictCounter = new GlobalConflictCounter(conflictCounterWidth);
        this.defaultConfig = new BetaTransactionConfiguration(this)
                .setSpinCount(spinCount);
        this.storage = new SimpleStorage(this);
        this.atomicBlock = createTransactionFactoryBuilder()
                .setSpeculativeConfigEnabled(false)
                .buildAtomicBlock();
    }

    public SimpleProfiler getSimpleProfiler() {
        return simpleProfiler;
    }

    public StmCallback getCallback() {
        return callback;
    }

    public Storage getStorage() {
        return storage;
    }

    @Override
    public BetaTransactionFactoryBuilder createTransactionFactoryBuilder() {
        return new BetaTransactionFactoryBuilderImpl(defaultConfig);
    }

    @Override
    public ReferenceFactoryBuilder getReferenceFactoryBuilder() {
        return new BetaReferenceFactoryBuilderImpl();
    }

    public int getSpinCount() {
        return spinCount;
    }

    public GlobalConflictCounter getGlobalConflictCounter() {
        return globalConflictCounter;
    }

    @Override
    public BetaTransaction startDefaultTransaction() {
        return new FatArrayTreeBetaTransaction(defaultConfig);
    }

    @Override
    public AtomicBlock getDefaultAtomicBlock() {
        return atomicBlock;
    }

    public int getMaxArrayTransactionSize() {
        return 20;
    }

    public final class BetaReferenceFactoryBuilderImpl implements BetaReferenceFactoryBuilder {
        @Override
        public BetaReferenceFactory build() {
            return new BetaReferenceFactoryImpl();
        }
    }

    public final class BetaReferenceFactoryImpl implements BetaReferenceFactory {
        @Override
        public BetaIntRef createIntRef(int value) {
            return new BetaIntRef(value);
        }

        @Override
        public BetaLongRef createLongRef(long value) {
            return new BetaLongRef(value);
        }

        @Override
        public <E> BetaRef<E> createRef(E value) {
            return new BetaRef<E>(value);
        }
    }

    public final class BetaTransactionFactoryBuilderImpl implements BetaTransactionFactoryBuilder {

        private final BetaTransactionConfiguration config;

        BetaTransactionFactoryBuilderImpl(final BetaTransactionConfiguration config) {
            this.config = config;
        }

        @Override
        public BetaTransactionConfiguration getTransactionConfiguration() {
            return config;
        }

        @Override
        public BetaTransactionFactoryBuilder addPermanentListener(TransactionLifecycleListener listener) {
            return new BetaTransactionFactoryBuilderImpl(config.addPermanentListener(listener));
        }

        @Override
        public BetaTransactionFactoryBuilder setFamilyName(String familyName) {
            if (config.familyName.equals(familyName)) {
                return this;
            }

            return new BetaTransactionFactoryBuilderImpl(config.setFamilyName(familyName));
        }

        @Override
        public BetaTransactionFactoryBuilder setPropagationLevel(final PropagationLevel level) {
            if (level == config.propagationLevel) {
                return this;
            }

            return new BetaTransactionFactoryBuilderImpl(config.setPropagationLevel(level));
        }

        @Override
        public BetaTransactionFactoryBuilder setBlockingAllowed(final boolean blockingAllowed) {
            if (blockingAllowed == config.blockingAllowed) {
                return this;
            }

            return new BetaTransactionFactoryBuilderImpl(config.setBlockingAllowed(blockingAllowed));
        }

        @Override
        public BetaTransactionFactoryBuilder setWriteSkewAllowed(final boolean writeSkewAllowed) {
            if (writeSkewAllowed == config.writeSkewAllowed) {
                return this;
            }

            return new BetaTransactionFactoryBuilderImpl(config.setWriteSkewAllowed(writeSkewAllowed));
        }

        @Override
        public BetaTransactionFactoryBuilder setTraceLevel(final TraceLevel traceLevel) {
            if (traceLevel == config.traceLevel) {
                return this;
            }

            return new BetaTransactionFactoryBuilderImpl(config.setTraceLevel(traceLevel));
        }

        @Override
        public BetaTransactionFactoryBuilder setTimeoutNs(final long timeoutNs) {
            if (timeoutNs == config.timeoutNs) {
                return this;
            }

            return new BetaTransactionFactoryBuilderImpl(config.setTimeoutNs(timeoutNs));
        }

        @Override
        public BetaTransactionFactoryBuilder setInterruptible(final boolean interruptible) {
            if (interruptible == config.interruptible) {
                return this;
            }

            return new BetaTransactionFactoryBuilderImpl(config.setInterruptible(interruptible));
        }

        @Override
        public BetaTransactionFactoryBuilder setBackoffPolicy(final BackoffPolicy backoffPolicy) {
            if (backoffPolicy == config.backoffPolicy) {
                return this;
            }

            return new BetaTransactionFactoryBuilderImpl(config.setBackoffPolicy(backoffPolicy));
        }

        @Override
        public BetaTransactionFactoryBuilder setPessimisticLockLevel(final PessimisticLockLevel lockLevel) {
            if (lockLevel == config.pessimisticLockLevel) {
                return this;
            }

            return new BetaTransactionFactoryBuilderImpl(config.setPessimisticLockLevel(lockLevel));
        }

        @Override
        public BetaTransactionFactoryBuilder setDirtyCheckEnabled(final boolean dirtyCheckEnabled) {
            if (dirtyCheckEnabled == config.dirtyCheck) {
                return this;
            }

            return new BetaTransactionFactoryBuilderImpl(config.setDirtyCheckEnabled(dirtyCheckEnabled));
        }

        @Override
        public BetaTransactionFactoryBuilder setSpinCount(final int spinCount) {
            if (spinCount == config.spinCount) {
                return this;
            }

            return new BetaTransactionFactoryBuilderImpl(config.setSpinCount(spinCount));
        }

        @Override
        public BetaTransactionFactoryBuilder setSpeculativeConfigEnabled(final boolean enabled) {
            if (enabled == config.speculativeConfigEnabled) {
                return this;
            }

            return new BetaTransactionFactoryBuilderImpl(
                    config.setSpeculativeConfigurationEnabled(enabled));
        }

        @Override
        public BetaTransactionFactoryBuilder setReadonly(final boolean readonly) {
            if (readonly == config.readonly) {
                return this;
            }

            return new BetaTransactionFactoryBuilderImpl(config.setReadonly(readonly));
        }

        @Override
        public BetaTransactionFactoryBuilder setReadTrackingEnabled(final boolean enabled) {
            if (enabled == config.trackReads) {
                return this;
            }

            return new BetaTransactionFactoryBuilderImpl(config.setReadTrackingEnabled(enabled));
        }

        @Override
        public BetaTransactionFactoryBuilder setMaxRetries(final int maxRetries) {
            if (maxRetries == config.maxRetries) {
                return this;
            }

            return new BetaTransactionFactoryBuilderImpl(config.setMaxRetries(maxRetries));
        }

        @Override
        public AtomicBlock buildAtomicBlock() {
            if (leanAtomicBlock()) {
                return new LeanBetaAtomicBlock(build());
            } else {
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

            return factory;
        }
    }

    public final class NonSpeculativeBetaTransactionFactory implements BetaTransactionFactory {

        private final BetaTransactionConfiguration config;

        NonSpeculativeBetaTransactionFactory(final BetaTransactionConfiguration config) {
            this.config = config;
        }

        @Override
        public BetaTransactionConfiguration getTransactionConfiguration() {
            return config;
        }

        @Override
        public BetaTransaction start() {
            return start(getThreadLocalBetaTransactionPool());
        }

        @Override
        public BetaTransaction start(final BetaTransactionPool pool) {
            FatArrayTreeBetaTransaction tx = pool.takeFatArrayTreeBetaTransaction();

            if (tx == null) {
                tx = new FatArrayTreeBetaTransaction(config);
            } else {
                tx.init(config);
            }

            return tx;
        }

        @Override
        public BetaTransaction upgradeAfterSpeculativeFailure(
                final BetaTransaction failingTransaction, final BetaTransactionPool pool) {
            throw new UnsupportedOperationException();
        }
    }

    public final class SpeculativeBetaTransactionFactory implements BetaTransactionFactory {

        private final BetaTransactionConfiguration config;

        SpeculativeBetaTransactionFactory(final BetaTransactionConfiguration config) {
            this.config = config;
        }

        @Override
        public BetaTransactionConfiguration getTransactionConfiguration() {
            return config;
        }

        @Override
        public BetaTransaction start() {
            return start(getThreadLocalBetaTransactionPool());
        }

        @Override
        public BetaTransaction upgradeAfterSpeculativeFailure(
                final BetaTransaction failingTransaction, final BetaTransactionPool pool) {
            final BetaTransaction tx = start(pool);
            tx.copyForSpeculativeFailure(failingTransaction);
            return tx;
        }

        @Override
        public BetaTransaction start(final BetaTransactionPool pool) {
            final SpeculativeBetaConfig speculativeConfig = config.getSpeculativeConfig();
            final int length = speculativeConfig.getMinimalLength();

            if (length <= 1) {
                if (speculativeConfig.isFat()) {
                    FatMonoBetaTransaction tx = pool.takeFatMonoBetaTransaction();
                    if (tx == null) {
                        tx = new FatMonoBetaTransaction(config);
                    } else {
                        tx.init(config);
                    }
                    return tx;
                } else {
                    LeanMonoBetaTransaction tx = pool.takeLeanMonoBetaTransaction();
                    if (tx == null) {
                        tx = new LeanMonoBetaTransaction(config);
                    } else {
                        tx.init(config);
                    }
                    return tx;
                }
            } else if (length <= config.getMaxArrayTransactionSize()) {
                if (speculativeConfig.isFat()) {
                    FatArrayBetaTransaction tx = pool.takeFatArrayBetaTransaction();
                    if (tx == null) {
                        tx = new FatArrayBetaTransaction(config);
                    } else {
                        tx.init(config);
                    }
                    return tx;
                } else {
                    LeanArrayBetaTransaction tx = pool.takeLeanArrayBetaTransaction();
                    if (tx == null) {
                        tx = new LeanArrayBetaTransaction(config);
                    } else {
                        tx.init(config);
                    }
                    return tx;
                }
            } else {
                if (speculativeConfig.isFat()) {
                    FatArrayTreeBetaTransaction tx = pool.takeFatArrayTreeBetaTransaction();
                    if (tx == null) {
                        tx = new FatArrayTreeBetaTransaction(config);
                    } else {
                        tx.init(config);
                    }
                    return tx;
                } else {
                    LeanArrayTreeBetaTransaction tx = pool.takeLeanArrayTreeBetaTransaction();
                    if (tx == null) {
                        tx = new LeanArrayTreeBetaTransaction(config);
                    } else {
                        tx.init(config);
                    }
                    return tx;
                }
            }
        }
    }
}
