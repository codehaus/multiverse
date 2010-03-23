package org.multiverse.stms.alpha;

import org.multiverse.api.Stm;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.TransactionFactoryBuilder;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.stms.alpha.transactions.OptimalSize;
import org.multiverse.stms.alpha.transactions.readonly.*;
import org.multiverse.stms.alpha.transactions.update.ArrayUpdateAlphaTransaction;
import org.multiverse.stms.alpha.transactions.update.MapUpdateAlphaTransaction;
import org.multiverse.stms.alpha.transactions.update.MonoUpdateAlphaTransaction;
import org.multiverse.stms.alpha.transactions.update.UpdateAlphaTransactionConfig;
import org.multiverse.utils.backoff.BackoffPolicy;
import org.multiverse.utils.clock.PrimitiveClock;
import org.multiverse.utils.commitlock.CommitLockPolicy;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static java.lang.String.format;

/**
 * Default {@link Stm} implementation that provides the most complete set of features. Like retry/orelse.
 * <p/>
 * It can be configured through the {@link AlphaStmConfig}.
 *
 * @author Peter Veentjer.
 */
public final class AlphaStm implements Stm<AlphaStm.AlphaTransactionFactoryBuilder> {

    private final static Logger logger = Logger.getLogger(AlphaStm.class.getName());

    private final ConcurrentMap<String, OptimalSize> sizeMap = new ConcurrentHashMap<String, OptimalSize>();

    private final PrimitiveClock clock;

    private final CommitLockPolicy commitLockPolicy;

    private final BackoffPolicy backoffPolicy;

    private final AlphaTransactionFactoryBuilder transactionBuilder;

    private final int maxRetryCount;

    private final int maxFixedUpdateSize;

    private final boolean smartTxLengthSelector;

    private final boolean optimizeConflictDetection;

    private final boolean dirtyCheck;

    public static AlphaStm createFast() {
        return new AlphaStm(AlphaStmConfig.createFastConfig());
    }

    public static AlphaStm createDebug() {
        return new AlphaStm(AlphaStmConfig.createDebugConfig());
    }

    /**
     * Creates a new AlphaStm with the AlphaStmConfig.createFast as configuration.
     */
    public AlphaStm() {
        this(AlphaStmConfig.createFastConfig());
    }

    /**
     * Creates a new AlphaStm with the provided configuration.
     *
     * @param config the provided config.
     * @throws NullPointerException  if config is null.
     * @throws IllegalStateException if the provided config is invalid.
     */
    public AlphaStm(AlphaStmConfig config) {
        if (config == null) {
            throw new NullPointerException();
        }

        config.ensureValid();

        this.smartTxLengthSelector = config.smartTxImplementationChoice;
        this.clock = config.clock;
        this.maxFixedUpdateSize = config.maxFixedUpdateSize;
        this.commitLockPolicy = config.commitLockPolicy;
        this.backoffPolicy = config.backoffPolicy;
        this.optimizeConflictDetection = config.optimizedConflictDetection;
        this.dirtyCheck = config.dirtyCheck;
        this.maxRetryCount = config.maxRetryCount;

        this.transactionBuilder = new AlphaTransactionFactoryBuilder();

        logger.info("Created a new AlphaStm instance");
    }

    @Override
    public AlphaTransactionFactoryBuilder getTransactionFactoryBuilder() {
        return transactionBuilder;
    }

    /**
     * Returns the current WriteSetLockPolicy. Returned value will never be null.
     *
     * @return the current WriteSetLockPolicy.
     */
    public CommitLockPolicy getAtomicObjectLockPolicy() {
        return commitLockPolicy;
    }


    /**
     * Returns the current BackoffPolicy. Returned value will never be null.
     *
     * @return
     */
    public BackoffPolicy getBackoffPolicy() {
        return backoffPolicy;
    }

    @Override
    public long getVersion() {
        return clock.getVersion();
    }

    public PrimitiveClock getClock() {
        return clock;
    }

    public class AlphaTransactionFactoryBuilder
            implements TransactionFactoryBuilder<AlphaTransaction, AlphaTransactionFactoryBuilder> {

        private final int maxRetryCount;
        private final boolean readonly;
        private final String familyName;
        private final boolean automaticReadTracking;
        private final boolean allowWriteSkewProblem;
        private final CommitLockPolicy commitLockPolicy;
        private final BackoffPolicy backoffPolicy;
        private final OptimalSize optimalSize;
        private final boolean interruptible;
        private final boolean speculativeConfiguration;
        private final boolean dirtyCheck;

        public AlphaTransactionFactoryBuilder() {
            this(false, true, null,
                    AlphaStm.this.maxRetryCount,
                    true,
                    AlphaStm.this.commitLockPolicy,
                    AlphaStm.this.backoffPolicy,
                    null,
                    false,
                    AlphaStm.this.smartTxLengthSelector,
                    AlphaStm.this.dirtyCheck);
        }

        public AlphaTransactionFactoryBuilder(
                boolean readonly, boolean automaticReadTracking, String familyName, int maxRetryCount,
                boolean allowWriteSkewProblem,
                CommitLockPolicy commitLockPolicy, BackoffPolicy backoffPolicy, OptimalSize optimalSize,
                boolean interruptible, boolean speculativeConfiguration, boolean dirtyCheck) {
            this.readonly = readonly;
            this.familyName = familyName;
            this.maxRetryCount = maxRetryCount;
            this.automaticReadTracking = automaticReadTracking;
            this.allowWriteSkewProblem = allowWriteSkewProblem;
            this.commitLockPolicy = commitLockPolicy;
            this.backoffPolicy = backoffPolicy;
            this.optimalSize = optimalSize;
            this.interruptible = interruptible;
            this.speculativeConfiguration = speculativeConfiguration;
            this.dirtyCheck = dirtyCheck;
        }

        @Override
        public AlphaTransactionFactoryBuilder setTimeout(long timeout, TimeUnit unit) {
            //todo: this needs to be done.            
            return this;
        }

        @Override
        public AlphaTransactionFactoryBuilder setFamilyName(String familyName) {
            if (familyName == null) {
                return new AlphaTransactionFactoryBuilder(
                        readonly, automaticReadTracking, null, maxRetryCount, allowWriteSkewProblem,
                        commitLockPolicy, backoffPolicy, null, interruptible, speculativeConfiguration, dirtyCheck);
            }

            OptimalSize optimalSize = sizeMap.get(familyName);
            if (optimalSize == null) {
                OptimalSize newOptimalSize = new OptimalSize(1, maxFixedUpdateSize);
                OptimalSize found = sizeMap.putIfAbsent(familyName, newOptimalSize);
                optimalSize = found == null ? newOptimalSize : found;
            }

            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, maxRetryCount, allowWriteSkewProblem, commitLockPolicy,
                    backoffPolicy, optimalSize, interruptible, speculativeConfiguration, dirtyCheck);
        }

        @Override
        public AlphaTransactionFactoryBuilder setMaxRetryCount(int retryCount) {
            if (retryCount < 0) {
                throw new IllegalArgumentException(format("retryCount can't be smaller than 0, found %s", retryCount));
            }

            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, retryCount, allowWriteSkewProblem, commitLockPolicy,
                    backoffPolicy, optimalSize, interruptible, speculativeConfiguration, dirtyCheck);
        }

        @Override
        public AlphaTransactionFactoryBuilder setReadonly(boolean readonly) {
            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, maxRetryCount, allowWriteSkewProblem, commitLockPolicy,
                    backoffPolicy, optimalSize, interruptible, speculativeConfiguration, dirtyCheck);
        }

        public AlphaTransactionFactoryBuilder setAutomaticReadTracking(boolean automaticReadTracking) {
            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, maxRetryCount, allowWriteSkewProblem, commitLockPolicy,
                    backoffPolicy, optimalSize, interruptible, speculativeConfiguration, dirtyCheck);
        }

        @Override
        public AlphaTransactionFactoryBuilder setInterruptible(boolean interruptible) {
            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, maxRetryCount, allowWriteSkewProblem, commitLockPolicy,
                    backoffPolicy, optimalSize, interruptible, speculativeConfiguration, dirtyCheck);
        }

        public AlphaTransactionFactoryBuilder setCommitLockPolicy(CommitLockPolicy commitLockPolicy) {
            if (commitLockPolicy == null) {
                throw new NullPointerException();
            }

            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, maxRetryCount, allowWriteSkewProblem, commitLockPolicy,
                    backoffPolicy, optimalSize, interruptible, speculativeConfiguration, dirtyCheck);
        }

        @Override
        public AlphaTransactionFactoryBuilder setSpeculativeConfigurationEnabled(boolean speculativeConfiguration) {
            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, maxRetryCount, allowWriteSkewProblem, commitLockPolicy,
                    backoffPolicy, optimalSize, interruptible, speculativeConfiguration, dirtyCheck);
        }

        @Override
        public AlphaTransactionFactoryBuilder setAllowWriteSkewProblem(boolean allowWriteSkew) {
            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, maxRetryCount, allowWriteSkew, commitLockPolicy,
                    backoffPolicy, optimalSize, interruptible, speculativeConfiguration, dirtyCheck);
        }

        @Override
        public AlphaTransactionFactoryBuilder setBackoffPolicy(BackoffPolicy backoffPolicy) {
            if (backoffPolicy == null) {
                throw new NullPointerException();
            }

            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, maxRetryCount, allowWriteSkewProblem, commitLockPolicy,
                    backoffPolicy, optimalSize, interruptible, speculativeConfiguration, dirtyCheck);
        }

        @Override
        public TransactionFactory<AlphaTransaction> build() {
            if (readonly) {
                return createReadonlyTxFactory();
            } else {
                if (!automaticReadTracking && !allowWriteSkewProblem) {
                    String msg = format("Can't create transactionfactory for transaction family '%s' because an update "
                            + "transaction without automaticReadTracking and without allowWriteSkewProblem is "
                            + "not possible", familyName
                    );

                    throw new IllegalStateException(msg);
                }

                return createUpdateTxFactory();
            }
        }

        private TransactionFactory<AlphaTransaction> createReadonlyTxFactory() {
            if (automaticReadTracking) {
                return createReadTrackingReadonlyTxFactory();
            } else {
                ReadonlyAlphaTransactionConfig config =
                        new ReadonlyAlphaTransactionConfig(
                                clock, backoffPolicy, familyName, optimalSize,
                                maxRetryCount, interruptible, false);

                return new NonTrackingReadonlyAlphaTransaction.Factory(config);
            }
        }

        private TransactionFactory<AlphaTransaction> createReadTrackingReadonlyTxFactory() {
            if (speculativeConfiguration) {
                return new TransactionFactory<AlphaTransaction>() {
                    ReadonlyAlphaTransactionConfig config =
                            new ReadonlyAlphaTransactionConfig(
                                    clock, backoffPolicy, familyName, optimalSize, maxRetryCount,
                                    interruptible, true);


                    @Override
                    public AlphaTransaction start() {
                        if (optimalSize == null) {
                            return new MapReadonlyAlphaTransaction(config);
                        }

                        int size = optimalSize.get();
                        if (size <= 1) {
                            return new MonoReadonlyAlphaTransaction(config);
                        } else if (size < maxFixedUpdateSize) {
                            return new ArrayReadonlyAlphaTransaction(config, size);
                        } else {
                            return new MapReadonlyAlphaTransaction(config);
                        }
                    }
                };
            } else {
                ReadonlyAlphaTransactionConfig config =
                        new ReadonlyAlphaTransactionConfig(
                                clock, backoffPolicy, familyName, optimalSize, maxRetryCount,
                                interruptible, true);
                return new MapReadonlyAlphaTransaction.Factory(config);
            }
        }

        private TransactionFactory<AlphaTransaction> createUpdateTxFactory() {
//            System.out.println("smartTxLengthSelector: " + smartTxLengthSelector);

            if (speculativeConfiguration) {
                return new TransactionFactory<AlphaTransaction>() {
                    UpdateAlphaTransactionConfig config =
                            new UpdateAlphaTransactionConfig(
                                    clock, backoffPolicy, commitLockPolicy, familyName,
                                    optimalSize, maxRetryCount, interruptible, automaticReadTracking,
                                    allowWriteSkewProblem, optimizeConflictDetection, true);

                    @Override
                    public AlphaTransaction start() {
                        if (optimalSize == null) {
                            return new MapUpdateAlphaTransaction(config);
                        }

                        int size = optimalSize.get();

                        if (size <= 1) {
                            return new MonoUpdateAlphaTransaction(config);
                        } else if (size <= maxFixedUpdateSize) {
                            return new ArrayUpdateAlphaTransaction(config, size);
                        } else {
                            return new MapUpdateAlphaTransaction(config);
                        }
                    }
                };
            } else {
                UpdateAlphaTransactionConfig config =
                        new UpdateAlphaTransactionConfig(
                                clock, backoffPolicy, commitLockPolicy, familyName,
                                optimalSize, maxRetryCount, interruptible,
                                automaticReadTracking, allowWriteSkewProblem,
                                optimizeConflictDetection, true);

                return new MapUpdateAlphaTransaction.Factory(config);
            }
        }
    }
}
