package org.multiverse.stms.gamma;

import org.multiverse.api.*;
import org.multiverse.api.collections.TransactionalCollectionsFactory;
import org.multiverse.api.lifecycle.TransactionListener;
import org.multiverse.collections.NaiveTransactionalCollectionFactory;
import org.multiverse.stms.gamma.transactionalobjects.*;
import org.multiverse.stms.gamma.transactions.*;
import org.multiverse.stms.gamma.transactions.fat.FatFixedLengthGammaTransaction;
import org.multiverse.stms.gamma.transactions.fat.FatMonoGammaTransaction;
import org.multiverse.stms.gamma.transactions.fat.FatVariableLengthGammaTransaction;
import org.multiverse.stms.gamma.transactions.lean.LeanFixedLengthGammaTransaction;
import org.multiverse.stms.gamma.transactions.lean.LeanMonoGammaTransaction;

import static org.multiverse.stms.gamma.transactions.ThreadLocalGammaTransactionPool.getThreadLocalGammaTransactionPool;


public final class GammaStm implements Stm {

    @SuppressWarnings({"UnusedDeclaration"})
    public static GammaStm createFast() {
        return new GammaStm();
    }

    public final int defaultMaxRetries;
    public final int spinCount;
    public final BackoffPolicy defaultBackoffPolicy;

    public final GlobalConflictCounter globalConflictCounter = new GlobalConflictCounter();
    public final GammaRefFactoryImpl defaultRefFactory = new GammaRefFactoryImpl();
    public final GammaRefFactoryBuilder refFactoryBuilder = new RefFactoryBuilderImpl();
    public final GammaAtomicBlock defaultAtomicBlock;
    public final GammaTransactionConfiguration defaultConfig;
    public final NaiveTransactionalCollectionFactory defaultTransactionalCollectionFactory
            = new NaiveTransactionalCollectionFactory(this);
    public final int readBiasedThreshold;


    public GammaStm() {
        this(new GammaStmConfiguration());
    }

    public GammaStm(GammaStmConfiguration configuration) {
        configuration.validate();

        this.defaultMaxRetries = configuration.maxRetries;
        this.spinCount = configuration.spinCount;
        this.defaultBackoffPolicy = configuration.backoffPolicy;
        this.defaultConfig = new GammaTransactionConfiguration(this, configuration)
                .setSpinCount(spinCount);
        this.defaultAtomicBlock = newTransactionFactoryBuilder()
                .setSpeculative(false)
                .newAtomicBlock();
        this.readBiasedThreshold = configuration.readBiasedThreshold;
    }

    @Override
    public GammaTransaction newDefaultTransaction() {
        return new FatVariableLengthGammaTransaction(this);
    }

    @Override
    public GammaAtomicBlock getDefaultAtomicBlock() {
        return defaultAtomicBlock;
    }

    @Override
    public OrElseBlock newOrElseBlock() {
        return null;
    }

    public GlobalConflictCounter getGlobalConflictCounter() {
        return globalConflictCounter;
    }

    public class GammaTransactionFactoryBuilderImpl implements GammaTransactionFactoryBuilder {

        private final GammaTransactionConfiguration config;

        GammaTransactionFactoryBuilderImpl(final GammaTransactionConfiguration config) {
            this.config = config;
        }

        @Override
        public GammaTransactionFactoryBuilder setFat() {
            if (config.isFat) {
                return this;
            }

            return new GammaTransactionFactoryBuilderImpl(config.setFat());
        }

        @Override
        public GammaTransactionConfiguration getTransactionConfiguration() {
            return config;
        }

        @Override
        public GammaTransactionFactoryBuilder addPermanentListener(TransactionListener listener) {
            return new GammaTransactionFactoryBuilderImpl(config.addPermanentListener(listener));
        }

        @Override
        public GammaTransactionFactoryBuilder setControlFlowErrorsReused(boolean reused) {
            if (config.controlFlowErrorsReused = reused) {
                return this;
            }

            return new GammaTransactionFactoryBuilderImpl(config.setControlFlowErrorsReused(reused));
        }

        @Override
        public GammaTransactionFactoryBuilder setReadLockMode(LockMode lockMode) {
            if (config.readLockMode == lockMode) {
                return this;
            }

            return new GammaTransactionFactoryBuilderImpl(config.setReadLockMode(lockMode));
        }

        @Override
        public GammaTransactionFactoryBuilder setWriteLockMode(LockMode lockMode) {
            if (config.writeLockMode == lockMode) {
                return this;
            }

            return new GammaTransactionFactoryBuilderImpl(config.setWriteLockMode(lockMode));
        }

        @Override
        public GammaTransactionFactoryBuilder setFamilyName(String familyName) {
            if (config.familyName.equals(familyName)) {
                return this;
            }

            return new GammaTransactionFactoryBuilderImpl(config.setFamilyName(familyName));
        }

        @Override
        public GammaTransactionFactoryBuilder setPropagationLevel(final PropagationLevel level) {
            if (level == config.propagationLevel) {
                return this;
            }

            return new GammaTransactionFactoryBuilderImpl(config.setPropagationLevel(level));
        }

        @Override
        public GammaTransactionFactoryBuilder setBlockingAllowed(final boolean blockingAllowed) {
            if (blockingAllowed == config.blockingAllowed) {
                return this;
            }

            return new GammaTransactionFactoryBuilderImpl(config.setBlockingAllowed(blockingAllowed));
        }

        @Override
        public GammaTransactionFactoryBuilder setIsolationLevel(final IsolationLevel isolationLevel) {
            if (isolationLevel == config.isolationLevel) {
                return this;
            }

            return new GammaTransactionFactoryBuilderImpl(config.setIsolationLevel(isolationLevel));
        }

        @Override
        public GammaTransactionFactoryBuilder setTraceLevel(final TraceLevel traceLevel) {
            if (traceLevel == config.traceLevel) {
                return this;
            }

            return new GammaTransactionFactoryBuilderImpl(config.setTraceLevel(traceLevel));
        }

        @Override
        public GammaTransactionFactoryBuilder setTimeoutNs(final long timeoutNs) {
            if (timeoutNs == config.timeoutNs) {
                return this;
            }

            return new GammaTransactionFactoryBuilderImpl(config.setTimeoutNs(timeoutNs));
        }

        @Override
        public GammaTransactionFactoryBuilder setInterruptible(final boolean interruptible) {
            if (interruptible == config.interruptible) {
                return this;
            }

            return new GammaTransactionFactoryBuilderImpl(config.setInterruptible(interruptible));
        }

        @Override
        public GammaTransactionFactoryBuilder setBackoffPolicy(final BackoffPolicy backoffPolicy) {
            //noinspection ObjectEquality
            if (backoffPolicy == config.backoffPolicy) {
                return this;
            }

            return new GammaTransactionFactoryBuilderImpl(config.setBackoffPolicy(backoffPolicy));
        }

        @Override
        public GammaTransactionFactoryBuilder setDirtyCheckEnabled(final boolean dirtyCheckEnabled) {
            if (dirtyCheckEnabled == config.dirtyCheck) {
                return this;
            }

            return new GammaTransactionFactoryBuilderImpl(config.setDirtyCheckEnabled(dirtyCheckEnabled));
        }

        @Override
        public GammaTransactionFactoryBuilder setSpinCount(final int spinCount) {
            if (spinCount == config.spinCount) {
                return this;
            }

            return new GammaTransactionFactoryBuilderImpl(config.setSpinCount(spinCount));
        }

        @Override
        public GammaTransactionFactoryBuilder setSpeculative(final boolean enabled) {
            if (enabled == config.speculative) {
                return this;
            }

            return new GammaTransactionFactoryBuilderImpl(
                    config.setSpeculative(enabled));
        }

        @Override
        public GammaTransactionFactoryBuilder setReadonly(final boolean readonly) {
            if (readonly == config.readonly) {
                return this;
            }

            return new GammaTransactionFactoryBuilderImpl(config.setReadonly(readonly));
        }

        @Override
        public GammaTransactionFactoryBuilder setReadTrackingEnabled(final boolean enabled) {
            if (enabled == config.trackReads) {
                return this;
            }

            return new GammaTransactionFactoryBuilderImpl(config.setReadTrackingEnabled(enabled));
        }

        @Override
        public GammaTransactionFactoryBuilder setMaxRetries(final int maxRetries) {
            if (maxRetries == config.maxRetries) {
                return this;
            }

            return new GammaTransactionFactoryBuilderImpl(config.setMaxRetries(maxRetries));
        }

        @Override
        public GammaAtomicBlock newAtomicBlock() {
            config.init();

            if (leanAtomicBlock()) {
                return new LeanGammaAtomicBlock(newTransactionFactory());
            } else {
                return new FatGammaAtomicBlock(newTransactionFactory());
            }
        }

        private boolean leanAtomicBlock() {
            return config.propagationLevel == PropagationLevel.Requires;
        }

        @Override
        public GammaTransactionFactory newTransactionFactory() {
            config.init();

            if (config.isSpeculative()) {
                return new SpeculativeGammaTransactionFactory(config);
            } else {
                return new NonSpeculativeGammaTransactionFactory(config);
            }
        }
    }

    @Override
    public GammaRefFactory getDefaultRefFactory() {
        return defaultRefFactory;
    }

    class GammaRefFactoryImpl implements GammaRefFactory {
        @Override
        public <E> GammaRef<E> newRef(E value) {
            return new GammaRef<E>(GammaStm.this, value);
        }

        @Override
        public GammaIntRef newIntRef(int value) {
            return new GammaIntRef(GammaStm.this, value);
        }

        @Override
        public GammaBooleanRef newBooleanRef(boolean value) {
            return new GammaBooleanRef(GammaStm.this, value);
        }

        @Override
        public GammaDoubleRef newDoubleRef(double value) {
            return new GammaDoubleRef(GammaStm.this, value);
        }

        @Override
        public GammaLongRef newLongRef(long value) {
            return new GammaLongRef(GammaStm.this, value);
        }
    }

    @Override
    public GammaTransactionFactoryBuilder newTransactionFactoryBuilder() {
        GammaTransactionConfiguration config = new GammaTransactionConfiguration(this);
        return new GammaTransactionFactoryBuilderImpl(config);
    }

    @Override
    public TransactionalCollectionsFactory getDefaultTransactionalCollectionFactory() {
        return defaultTransactionalCollectionFactory;
    }

    @Override
    public GammaRefFactoryBuilder getRefFactoryBuilder() {
        return refFactoryBuilder;
    }

    public final class RefFactoryBuilderImpl implements GammaRefFactoryBuilder {
        @Override
        public GammaRefFactory build() {
            return new GammaRefFactoryImpl();
        }
    }

    public static final class NonSpeculativeGammaTransactionFactory implements GammaTransactionFactory {

        private final GammaTransactionConfiguration config;

        NonSpeculativeGammaTransactionFactory(final GammaTransactionConfiguration config) {
            this.config = config.init();
        }

        @Override
        public GammaTransactionConfiguration getConfiguration() {
            return config;
        }

        @Override
        public GammaTransaction newTransaction() {
            return newTransaction(getThreadLocalGammaTransactionPool());
        }

        @Override
        public GammaTransaction newTransaction(final GammaTransactionPool pool) {
            FatVariableLengthGammaTransaction tx = pool.takeMap();

            if (tx == null) {
                tx = new FatVariableLengthGammaTransaction(config);
            } else {
                tx.init(config);
            }

            return tx;
        }

        @Override
        public GammaTransaction upgradeAfterSpeculativeFailure(final GammaTransaction tailingTx, final GammaTransactionPool pool) {
            throw new UnsupportedOperationException();
        }
    }

    public static final class SpeculativeGammaTransactionFactory implements GammaTransactionFactory {

        private final GammaTransactionConfiguration config;

        SpeculativeGammaTransactionFactory(final GammaTransactionConfiguration config) {
            this.config = config.init();
        }

        @Override
        public GammaTransactionConfiguration getConfiguration() {
            return config;
        }

        @Override
        public GammaTransaction newTransaction() {
            return newTransaction(getThreadLocalGammaTransactionPool());
        }

        @Override
        public GammaTransaction upgradeAfterSpeculativeFailure(final GammaTransaction failingTx, final GammaTransactionPool pool) {
            final GammaTransaction tx = newTransaction(pool);
            tx.copyForSpeculativeFailure(failingTx);
            return tx;
        }

        @Override
        public GammaTransaction newTransaction(final GammaTransactionPool pool) {
            final SpeculativeGammaConfiguration speculativeConfiguration = config.speculativeConfiguration.get();
            final int length = speculativeConfiguration.minimalLength;

            if (length <= 1) {
                if (speculativeConfiguration.isFat) {
                    FatMonoGammaTransaction tx = pool.takeFatMono();
                    if (tx == null) {
                        return new FatMonoGammaTransaction(config);
                    }

                    tx.init(config);
                    return tx;
                } else {
                    LeanMonoGammaTransaction tx = pool.takeLeanMono();
                    if (tx == null) {
                        return new LeanMonoGammaTransaction(config);
                    }

                    tx.init(config);
                    return tx;
                }

            } else if (length <= config.maxFixedLengthTransactionSize) {
                if (speculativeConfiguration.isFat) {
                    final FatFixedLengthGammaTransaction tx = pool.takeFatFixedLength();
                    if (tx == null) {
                        return new FatFixedLengthGammaTransaction(config);
                    }

                    tx.init(config);
                    return tx;
                } else {
                    final LeanFixedLengthGammaTransaction tx = pool.takeLeanFixedLength();
                    if (tx == null) {
                        return new LeanFixedLengthGammaTransaction(config);
                    }

                    tx.init(config);
                    return tx;
                }

            } else {
                final FatVariableLengthGammaTransaction tx = pool.takeMap();
                if (tx == null) {
                    return new FatVariableLengthGammaTransaction(config);
                }

                tx.init(config);
                return tx;
            }
        }
    }
}
