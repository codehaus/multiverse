package org.multiverse.stms.beta;

import org.multiverse.api.*;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.conflictcounters.GlobalConflictCounter;
import org.multiverse.stms.beta.transactionalobjects.*;
import org.multiverse.stms.beta.transactions.*;

import static org.multiverse.stms.beta.ThreadLocalBetaTransactionPool.getThreadLocalBetaTransactionPool;

/**
 * @author Peter Veentjer
 */
public final class BetaStm implements Stm {

    public static BetaStm createFast() {
        return new BetaStm(new BetaStmConfiguration());
    }

    public final AtomicBlock defaultAtomicBlock;
    public final GlobalConflictCounter globalConflictCounter;
    public final int spinCount;
    public final BetaTransactionConfiguration defaultConfig;
    public final BackoffPolicy defaultBackoffPolicy;
    public final int defaultMaxRetries;
    public final BetaRefFactoryImpl defaultRefFactory = new BetaRefFactoryImpl();

    public BetaStm() {
        this(new BetaStmConfiguration());
    }

    public BetaStm(BetaStmConfiguration configuration) {
        configuration.validate();

        this.spinCount = configuration.spinCount;
        this.defaultMaxRetries = configuration.maxRetries;
        this.defaultBackoffPolicy = configuration.backoffPolicy;
        this.globalConflictCounter = new GlobalConflictCounter(1);
        this.defaultConfig = new BetaTransactionConfiguration(this, configuration)
                .setSpinCount(spinCount);
        this.defaultAtomicBlock = createTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .buildAtomicBlock();
    }

    @Override
    public BetaTransactionFactoryBuilder createTransactionFactoryBuilder() {
        return new BetaTransactionFactoryBuilderImpl(defaultConfig);
    }

    @Override
    public BetaRefFactoryBuilder getReferenceFactoryBuilder() {
        return new BetaRefFactoryBuilderImpl();
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
        return defaultAtomicBlock;
    }

    public final class BetaRefFactoryBuilderImpl implements BetaRefFactoryBuilder {
        @Override
        public BetaRefFactory build() {
            return new BetaRefFactoryImpl();
        }
    }

    @Override
    public BetaRefFactory getDefaultRefFactory() {
        return defaultRefFactory;
    }

    @Override
    public OrElseBlock createOrElseBlock() {
        throw new TodoException();
    }

    public final class BetaRefFactoryImpl implements BetaRefFactory {

        @Override
        public BetaBooleanRef newBooleanRef(boolean value) {
            return new BetaBooleanRef(BetaStm.this, value);
        }

        @Override
        public BetaDoubleRef newDoubleRef(double value) {
            return new BetaDoubleRef(BetaStm.this, value);
        }

        @Override
        public BetaIntRef newIntRef(int value) {
            return new BetaIntRef(BetaStm.this, value);
        }

        @Override
        public BetaLongRef newLongRef(long value) {
            return new BetaLongRef(BetaStm.this, value);
        }

        @Override
        public <E> BetaRef<E> newRef(E value) {
            return new BetaRef<E>(BetaStm.this, value);
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
        public BetaTransactionFactoryBuilder setIsolationLevel(final IsolationLevel isolationLevel) {
            if (isolationLevel == config.isolationLevel) {
                return this;
            }

            return new BetaTransactionFactoryBuilderImpl(config.setIsolationLevel(isolationLevel));
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
        public BetaTransactionFactoryBuilder setLockLevel(final LockLevel lockLevel) {
            if (lockLevel == config.lockLevel) {
                return this;
            }

            return new BetaTransactionFactoryBuilderImpl(config.setLockLevel(lockLevel));
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
        public BetaTransactionFactoryBuilder setSpeculativeConfigurationEnabled(final boolean enabled) {
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
            config.init();

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
            config.init();

            if (config.isSpeculativeConfigEnabled()) {
                return new SpeculativeBetaTransactionFactory(config);
            } else {
                return new NonSpeculativeBetaTransactionFactory(config);
            }
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
        public BetaTransaction newTransaction() {
            return newTransaction(getThreadLocalBetaTransactionPool());
        }

        @Override
        public BetaTransaction newTransaction(final BetaTransactionPool pool) {
            FatArrayTreeBetaTransaction tx = pool.takeFatArrayTreeBetaTransaction();

            if (tx == null) {
                tx = new FatArrayTreeBetaTransaction(config);
            } else {
                tx.init(config);
            }

            return tx;
        }

        @Override
        public BetaTransaction upgradeAfterSpeculativeFailure(final BetaTransaction tailingTx, final BetaTransactionPool pool) {
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
        public BetaTransaction newTransaction() {
            return newTransaction(getThreadLocalBetaTransactionPool());
        }

        @Override
        public BetaTransaction upgradeAfterSpeculativeFailure(final BetaTransaction failingTx, final BetaTransactionPool pool) {
            final BetaTransaction tx = newTransaction(pool);
            tx.copyForSpeculativeFailure(failingTx);
            return tx;
        }

        @Override
        public BetaTransaction newTransaction(final BetaTransactionPool pool) {
            final SpeculativeBetaConfiguration speculativeConfiguration = config.speculativeConfiguration.get();
            final int length = speculativeConfiguration.minimalLength;

            if (length <= 1) {
                if (speculativeConfiguration.isFat) {
                    final FatMonoBetaTransaction tx = pool.takeFatMonoBetaTransaction();
                    if (tx == null) {
                        return new FatMonoBetaTransaction(config);
                    }

                    tx.init(config);
                    return tx;
                } else {
                    LeanMonoBetaTransaction tx = pool.takeLeanMonoBetaTransaction();
                    if (tx == null) {
                        return new LeanMonoBetaTransaction(config);
                    }

                    tx.init(config);
                    return tx;
                }
            } else if (length <= config.maxArrayTransactionSize) {
                if (speculativeConfiguration.isFat) {
                    final FatArrayBetaTransaction tx = pool.takeFatArrayBetaTransaction();
                    if (tx == null) {
                        return new FatArrayBetaTransaction(config);
                    }

                    tx.init(config);
                    return tx;
                } else {
                    final LeanArrayBetaTransaction tx = pool.takeLeanArrayBetaTransaction();
                    if (tx == null) {
                        return new LeanArrayBetaTransaction(config);
                    }

                    tx.init(config);
                    return tx;
                }
            } else {
                if (speculativeConfiguration.isFat) {
                    final FatArrayTreeBetaTransaction tx = pool.takeFatArrayTreeBetaTransaction();
                    if (tx == null) {
                        return new FatArrayTreeBetaTransaction(config);
                    }

                    tx.init(config);
                    return tx;
                } else {
                    final LeanArrayTreeBetaTransaction tx = pool.takeLeanArrayTreeBetaTransaction();
                    if (tx == null) {
                        return new LeanArrayTreeBetaTransaction(config);
                    }

                    tx.init(config);
                    return tx;
                }
            }
        }
    }
}
