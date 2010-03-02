package org.multiverse.stms.alpha;

import org.multiverse.api.Stm;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.TransactionFactoryBuilder;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.stms.alpha.transactions.OptimalSize;
import org.multiverse.stms.alpha.transactions.readonly.FixedReadonlyAlphaTransaction;
import org.multiverse.stms.alpha.transactions.readonly.GrowingReadonlyAlphaTransaction;
import org.multiverse.stms.alpha.transactions.readonly.NonTrackingReadonlyAlphaTransaction;
import org.multiverse.stms.alpha.transactions.readonly.TinyReadonlyAlphaTransaction;
import org.multiverse.stms.alpha.transactions.update.FixedUpdateAlphaTransaction;
import org.multiverse.stms.alpha.transactions.update.GrowingUpdateAlphaTransaction;
import org.multiverse.stms.alpha.transactions.update.TinyUpdateAlphaTransaction;
import org.multiverse.utils.backoff.BackoffPolicy;
import org.multiverse.utils.clock.PrimitiveClock;
import org.multiverse.utils.commitlock.CommitLockPolicy;
import org.multiverse.utils.profiling.ProfileRepository;

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

    private final ProfileRepository profiler;

    private final CommitLockPolicy commitLockPolicy;

    private final BackoffPolicy backoffPolicy;

    private final AlphaTransactionFactoryBuilder transactionBuilder;

    private final int maxRetryCount;

    private final int maxFixedUpdateSize;

    private final boolean smartTxLengthSelector;

    private final boolean optimizeConflictDetection;

    private final boolean dirtyCheck;

    private final boolean interruptible;

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
        this.profiler = config.profiler;
        this.clock = config.clock;
        this.maxFixedUpdateSize = config.maxFixedUpdateSize;
        this.commitLockPolicy = config.commitLockPolicy;
        this.backoffPolicy = config.backoffPolicy;
        this.optimizeConflictDetection = config.optimizedConflictDetection;
        this.dirtyCheck = config.dirtyCheck;
        this.maxRetryCount = config.maxRetryCount;
        this.interruptible = config.interruptible;

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

    public class AlphaTransactionFactoryBuilder
            implements TransactionFactoryBuilder<AlphaTransaction, AlphaTransactionFactoryBuilder> {

        private final int maxRetryCount;
        private final boolean readonly;
        private final String familyName;
        private final boolean automaticReadTracking;
        private final boolean enableWriteSkewProblem;
        private final CommitLockPolicy commitLockPolicy;
        private final BackoffPolicy backoffPolicy;
        private final OptimalSize optimalSize;
        private final boolean interruptible;
        private final boolean smartTxLengthSelector;
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
                boolean enableWriteSkewProblem,
                CommitLockPolicy commitLockPolicy, BackoffPolicy backoffPolicy, OptimalSize optimalSize,
                boolean interruptible, boolean smartTxLengthSelector, boolean dirtyCheck) {
            this.readonly = readonly;
            this.familyName = familyName;
            this.maxRetryCount = maxRetryCount;
            this.automaticReadTracking = automaticReadTracking;
            this.enableWriteSkewProblem = enableWriteSkewProblem;
            this.commitLockPolicy = commitLockPolicy;
            this.backoffPolicy = backoffPolicy;
            this.optimalSize = optimalSize;
            this.interruptible = interruptible;
            this.smartTxLengthSelector = smartTxLengthSelector;
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
                        readonly, automaticReadTracking, null, maxRetryCount, enableWriteSkewProblem,
                        commitLockPolicy, backoffPolicy, null, interruptible, smartTxLengthSelector, dirtyCheck);
            }

            OptimalSize optimalSize = sizeMap.get(familyName);
            if (optimalSize == null) {
                OptimalSize newOptimalSize = new OptimalSize(1);
                OptimalSize found = sizeMap.putIfAbsent(familyName, newOptimalSize);
                optimalSize = found == null ? newOptimalSize : found;
            }

            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, maxRetryCount, enableWriteSkewProblem, commitLockPolicy,
                    backoffPolicy, optimalSize, interruptible, smartTxLengthSelector, dirtyCheck);
        }

        @Override
        public AlphaTransactionFactoryBuilder setMaxRetryCount(int retryCount) {
            if (retryCount < 0) {
                throw new IllegalArgumentException(format("retryCount can't be smaller than 0, found %s", retryCount));
            }

            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, retryCount, enableWriteSkewProblem, commitLockPolicy,
                    backoffPolicy, optimalSize, interruptible, smartTxLengthSelector, dirtyCheck);
        }

        @Override
        public AlphaTransactionFactoryBuilder setReadonly(boolean readonly) {
            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, maxRetryCount, enableWriteSkewProblem, commitLockPolicy,
                    backoffPolicy, optimalSize, interruptible, smartTxLengthSelector, dirtyCheck);
        }

        public AlphaTransactionFactoryBuilder setAutomaticReadTracking(boolean automaticReadTracking) {
            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, maxRetryCount, enableWriteSkewProblem, commitLockPolicy,
                    backoffPolicy, optimalSize, interruptible, smartTxLengthSelector, dirtyCheck);
        }

        @Override
        public AlphaTransactionFactoryBuilder setInterruptible(boolean interruptible) {
            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, maxRetryCount, enableWriteSkewProblem, commitLockPolicy,
                    backoffPolicy, optimalSize, interruptible, smartTxLengthSelector, dirtyCheck);
        }

        public AlphaTransactionFactoryBuilder setCommitLockPolicy(CommitLockPolicy commitLockPolicy) {
            if (commitLockPolicy == null) {
                throw new NullPointerException();
            }

            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, maxRetryCount, enableWriteSkewProblem, commitLockPolicy,
                    backoffPolicy, optimalSize, interruptible, smartTxLengthSelector, dirtyCheck);
        }

        @Override
        public AlphaTransactionFactoryBuilder setSmartTxLengthSelector(boolean smartTxLengthSelector) {
            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, maxRetryCount, enableWriteSkewProblem, commitLockPolicy,
                    backoffPolicy, optimalSize, interruptible, smartTxLengthSelector, dirtyCheck);
        }

        @Override
        public AlphaTransactionFactoryBuilder setAllowWriteSkewProblem(boolean allowWriteSkew) {
            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, maxRetryCount, allowWriteSkew, commitLockPolicy,
                    backoffPolicy, optimalSize, interruptible, smartTxLengthSelector, dirtyCheck);
        }

        @Override
        public AlphaTransactionFactoryBuilder setBackoffPolicy(BackoffPolicy backoffPolicy) {
            if (backoffPolicy == null) {
                throw new NullPointerException();
            }

            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, maxRetryCount, enableWriteSkewProblem, commitLockPolicy,
                    backoffPolicy, optimalSize, interruptible, smartTxLengthSelector, dirtyCheck);
        }

        @Override
        public TransactionFactory<AlphaTransaction> build() {
            if (readonly) {
                return createReadonlyTxFactory();
            } else {
                if (!automaticReadTracking && !enableWriteSkewProblem) {
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
                NonTrackingReadonlyAlphaTransaction.Config config = new NonTrackingReadonlyAlphaTransaction.Config(
                        clock, backoffPolicy, familyName, profiler, maxRetryCount);
                return new NonTrackingReadonlyAlphaTransaction.Factory(config);
            }
        }

        private TransactionFactory<AlphaTransaction> createReadTrackingReadonlyTxFactory() {
            if (smartTxLengthSelector) {
                return new TransactionFactory<AlphaTransaction>() {
                    GrowingReadonlyAlphaTransaction.Config growingConfig =
                            new GrowingReadonlyAlphaTransaction.Config(
                                    clock, backoffPolicy, familyName, profiler, maxRetryCount, interruptible);

                    FixedReadonlyAlphaTransaction.Config fixedConfig =
                            new FixedReadonlyAlphaTransaction.Config(
                                    clock, backoffPolicy, familyName, profiler, maxRetryCount, interruptible,
                                    optimalSize, maxFixedUpdateSize);

                    TinyReadonlyAlphaTransaction.Config tinyConfig =
                            new TinyReadonlyAlphaTransaction.Config(
                                    clock, backoffPolicy, familyName, profiler, maxRetryCount, interruptible,
                                    optimalSize);

                    @Override
                    public AlphaTransaction start() {
                        if (optimalSize == null) {
                            return new GrowingReadonlyAlphaTransaction(growingConfig);
                        }

                        int size = optimalSize.get();
                        if (size == 1) {
                            return new TinyReadonlyAlphaTransaction(tinyConfig);
                        } else if (size < maxFixedUpdateSize) {
                            return new FixedReadonlyAlphaTransaction(fixedConfig, size);
                        } else {
                            return new GrowingReadonlyAlphaTransaction(growingConfig);
                        }
                    }
                };
            } else {
                GrowingReadonlyAlphaTransaction.Config config = new GrowingReadonlyAlphaTransaction.Config(
                        clock, backoffPolicy, familyName, profiler, maxRetryCount, interruptible);
                return new GrowingReadonlyAlphaTransaction.Factory(config);
            }
        }

        private TransactionFactory<AlphaTransaction> createUpdateTxFactory() {
//            System.out.println("smartTxLengthSelector: " + smartTxLengthSelector);

            if (smartTxLengthSelector) {
                return new TransactionFactory<AlphaTransaction>() {
                    GrowingUpdateAlphaTransaction.Config growingConfig =
                            new GrowingUpdateAlphaTransaction.Config(
                                    clock, backoffPolicy, familyName, profiler, commitLockPolicy,
                                    maxRetryCount, enableWriteSkewProblem, interruptible, optimizeConflictDetection, true,
                                    automaticReadTracking);

                    FixedUpdateAlphaTransaction.Config fixedConfig =
                            new FixedUpdateAlphaTransaction.Config(
                                    clock, backoffPolicy, familyName, profiler, commitLockPolicy,
                                    maxRetryCount, enableWriteSkewProblem, optimalSize, interruptible,
                                    optimizeConflictDetection, true, automaticReadTracking, maxFixedUpdateSize);

                    TinyUpdateAlphaTransaction.Config tinyConfig =
                            new TinyUpdateAlphaTransaction.Config(
                                    clock, backoffPolicy, familyName, profiler, maxRetryCount,
                                    commitLockPolicy, interruptible, optimalSize, enableWriteSkewProblem,
                                    optimizeConflictDetection, true, automaticReadTracking);

                    @Override
                    public AlphaTransaction start() {
                        if (optimalSize == null) {
                            return new GrowingUpdateAlphaTransaction(growingConfig);
                        }

                        int size = optimalSize.get();
                        if (size == 1) {
                            return new TinyUpdateAlphaTransaction(tinyConfig);
                        } else if (size <= maxFixedUpdateSize) {
                            return new FixedUpdateAlphaTransaction(fixedConfig, size);
                        } else {
                            return new GrowingUpdateAlphaTransaction(growingConfig);
                        }
                    }
                };
            } else {
                GrowingUpdateAlphaTransaction.Config config =
                        new GrowingUpdateAlphaTransaction.Config(
                                clock, backoffPolicy, familyName, profiler, commitLockPolicy,
                                maxRetryCount, enableWriteSkewProblem, interruptible, optimizeConflictDetection, true,
                                automaticReadTracking);

                return new GrowingUpdateAlphaTransaction.Factory(config);
            }
        }
    }
}
