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
import org.multiverse.utils.TodoException;
import org.multiverse.utils.backoff.BackoffPolicy;
import org.multiverse.utils.clock.PrimitiveClock;
import org.multiverse.utils.commitlock.CommitLockPolicy;
import org.multiverse.utils.profiling.ProfileRepository;
import org.multiverse.utils.profiling.ProfilerAware;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import static java.lang.String.format;

/**
 * Default {@link Stm} implementation that provides the most complete set of features. Like retry/orelse, profiling,
 * etc. It can be configured through the {@link AlphaStmConfig}.
 * <p/>
 * <h3>Statistics</h3> This implementation can use {@link org.multiverse.utils.profiling.ProfileRepository}. This choice
 * needs to be made when the STM is constructed, so that the JIT can remove calls to the Profiler completely if a null
 * value is passed. The JIT is able to completely remove the following:
 * <pre>
 * if(profiler!=null){
 *      profiler.incSomeCounter();
 * }
 * </pre>
 * So if you are not using the profiler, you don't need to pay for it.
 * <p/>
 * The instrumentation is added directly to the code. Although it is less pretty, adding some form of external mechanism
 * to add this functionality is going to complicate matters (not at least deployment issues).
 * <p/>
 * <h3>Logging</h3> Logging to java.logging can be enabled through the constructor.
 * <p/>
 * The logging can be completely removed by the JIT if the loggingPossible flag is set to false. No additional checks
 * are done.. so you don't need to pay the price for it if you don't use it.
 *
 * @author Peter Veentjer.
 */
public final class AlphaStm implements Stm<AlphaStm.AlphaTransactionFactoryBuilder>, ProfilerAware {

    private final static Logger logger = Logger.getLogger(AlphaStm.class.getName());

    private final PrimitiveClock clock;

    private final ProfileRepository profiler;

    private final CommitLockPolicy commitLockPolicy;

    private final BackoffPolicy backoffPolicy;

    private final AlphaTransactionFactoryBuilder transactionBuilder;

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
     * Creates a new AlphaStm with the AlphaStmConfig.createDebugConfig as configuration.
     */
    public AlphaStm() {
        this(AlphaStmConfig.createDebugConfig());
    }


    @Override
    public AlphaTransactionFactoryBuilder getTransactionFactoryBuilder() {
        return transactionBuilder;
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
        //the abstracttransaction requires the clock to be at least 1, requirement from the
        //abstracttransaction.
        if (clock.getVersion() == 0) {
            clock.tick();
        }
        this.maxFixedUpdateSize = config.maxFixedUpdateSize;
        this.commitLockPolicy = config.commitLockPolicy;
        this.backoffPolicy = config.backoffPolicy;
        this.optimizeConflictDetection = config.optimizedConflictDetection;
        this.transactionBuilder = new AlphaTransactionFactoryBuilder();
        this.dirtyCheck = config.dirtyCheck;

        logger.info("Created a new AlphaStm instance");
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
    public BackoffPolicy getRestartBackoffPolicy() {
        return backoffPolicy;
    }

    /**
     * Returns the DefaultStmStatistics or null if the Stm is running without statistics.
     *
     * @return return the TL2StmStatistics.
     */
    public ProfileRepository getProfiler() {
        return profiler;
    }

    @Override
    public long getVersion() {
        return clock.getVersion();
    }

    private final ConcurrentMap<String, OptimalSize> sizeMap = new ConcurrentHashMap<String, OptimalSize>();

    public class AlphaTransactionFactoryBuilder
            implements TransactionFactoryBuilder<AlphaTransaction, AlphaTransactionFactoryBuilder> {

        private final int maxRetryCount;
        private final boolean readonly;
        private final String familyName;
        private final boolean automaticReadTracking;
        private final boolean preventWriteSkew;
        private final CommitLockPolicy commitLockPolicy;
        private final BackoffPolicy backoffPolicy;
        private final OptimalSize optimalSize;
        private final boolean interruptible;
        private final boolean smartTxLengthSelector;
        private final boolean dirtyCheck;

        public AlphaTransactionFactoryBuilder() {
            this(false, true, null, 1000, false, AlphaStm.this.commitLockPolicy,
                    AlphaStm.this.backoffPolicy,
                    null,
                    false,
                    AlphaStm.this.smartTxLengthSelector,
                    AlphaStm.this.dirtyCheck);
        }

        public AlphaTransactionFactoryBuilder(
                boolean readonly, boolean automaticReadTracking, String familyName, int maxRetryCount,
                boolean preventWriteSkew,
                CommitLockPolicy commitLockPolicy, BackoffPolicy backoffPolicy, OptimalSize optimalSize,
                boolean interruptible, boolean smartTxLengthSelector, boolean dirtyCheck) {
            this.readonly = readonly;
            this.familyName = familyName;
            this.maxRetryCount = maxRetryCount;
            this.automaticReadTracking = automaticReadTracking;
            this.preventWriteSkew = preventWriteSkew;
            this.commitLockPolicy = commitLockPolicy;
            this.backoffPolicy = backoffPolicy;
            this.optimalSize = optimalSize;
            this.interruptible = interruptible;
            this.smartTxLengthSelector = smartTxLengthSelector;
            this.dirtyCheck = dirtyCheck;
        }


        @Override
        public AlphaTransactionFactoryBuilder setFamilyName(String familyName) {
            if (familyName == null) {
                return new AlphaTransactionFactoryBuilder(
                        readonly, automaticReadTracking, null, maxRetryCount, preventWriteSkew,
                        commitLockPolicy, backoffPolicy, null, interruptible, smartTxLengthSelector, dirtyCheck);
            }

            OptimalSize optimalSize = sizeMap.get(familyName);
            if (optimalSize == null) {
                OptimalSize newOptimalSize = new OptimalSize(1);
                OptimalSize found = sizeMap.putIfAbsent(familyName, newOptimalSize);
                optimalSize = found == null ? newOptimalSize : found;
            }

            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, maxRetryCount, preventWriteSkew, commitLockPolicy,
                    backoffPolicy, optimalSize, interruptible, smartTxLengthSelector, dirtyCheck);
        }

        @Override
        public AlphaTransactionFactoryBuilder setMaxRetryCount(int retryCount) {
            if (retryCount < 0) {
                throw new IllegalArgumentException(format("retryCount can't be smaller than 0, found %s", retryCount));
            }

            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, retryCount, preventWriteSkew, commitLockPolicy,
                    backoffPolicy, optimalSize, interruptible, smartTxLengthSelector, dirtyCheck);
        }

        @Override
        public AlphaTransactionFactoryBuilder setReadonly(boolean readonly) {
            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, maxRetryCount, preventWriteSkew, commitLockPolicy,
                    backoffPolicy, optimalSize, interruptible, smartTxLengthSelector, dirtyCheck);
        }

        public AlphaTransactionFactoryBuilder setAutomaticReadTracking(boolean automaticReadTracking) {
            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, maxRetryCount, preventWriteSkew, commitLockPolicy,
                    backoffPolicy, optimalSize, interruptible, smartTxLengthSelector, dirtyCheck);
        }

        @Override
        public AlphaTransactionFactoryBuilder setInterruptible(boolean interruptible) {
            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, maxRetryCount, preventWriteSkew, commitLockPolicy,
                    backoffPolicy, optimalSize, interruptible, smartTxLengthSelector, dirtyCheck);
        }

        public AlphaTransactionFactoryBuilder setRestartBackoffPolicy(BackoffPolicy backoffPolicy) {
            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, maxRetryCount, preventWriteSkew, commitLockPolicy,
                    backoffPolicy, optimalSize, interruptible, smartTxLengthSelector, dirtyCheck);
        }


        public AlphaTransactionFactoryBuilder setCommitLockPolicy(CommitLockPolicy commitLockPolicy) {
            if (commitLockPolicy == null) {
                throw new NullPointerException();
            }

            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, maxRetryCount, preventWriteSkew, commitLockPolicy,
                    backoffPolicy, optimalSize, interruptible, smartTxLengthSelector, dirtyCheck);
        }


        @Override
        public AlphaTransactionFactoryBuilder setSmartTxLengthSelector(boolean smartTxLengthSelector) {
            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, maxRetryCount, preventWriteSkew, commitLockPolicy,
                    backoffPolicy, optimalSize, interruptible, smartTxLengthSelector, dirtyCheck);
        }

        public AlphaTransactionFactoryBuilder setLogging(boolean logging) {
            throw new TodoException();
        }

        @Override
        public AlphaTransactionFactoryBuilder setPreventWriteSkew(boolean preventWriteSkew) {
            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, maxRetryCount, preventWriteSkew, commitLockPolicy,
                    backoffPolicy, optimalSize, interruptible, smartTxLengthSelector, dirtyCheck);
        }

        @Override
        public TransactionFactory<AlphaTransaction> build() {
            if (readonly) {
                return createReadonlyTxFactory();
            } else {
                if (!automaticReadTracking && preventWriteSkew) {
                    throw new IllegalStateException(
                            format("Can't create transactionfactory for transaction family '%s' because an update "
                                    + "transaction without automaticReadTracking but with preventWriteSkew is "
                                    + "not possible", familyName
                            ));
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
                                    maxRetryCount, preventWriteSkew, interruptible, optimizeConflictDetection, true,
                                    automaticReadTracking);

                    FixedUpdateAlphaTransaction.Config fixedConfig =
                            new FixedUpdateAlphaTransaction.Config(
                                    clock, backoffPolicy, familyName, profiler, commitLockPolicy,
                                    maxRetryCount, preventWriteSkew, optimalSize, interruptible,
                                    optimizeConflictDetection, true, automaticReadTracking, maxFixedUpdateSize);

                    TinyUpdateAlphaTransaction.Config tinyConfig =
                            new TinyUpdateAlphaTransaction.Config(
                                    clock, backoffPolicy, familyName, profiler, maxRetryCount,
                                    commitLockPolicy, interruptible, optimalSize, preventWriteSkew,
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
                                maxRetryCount, preventWriteSkew, interruptible, optimizeConflictDetection, true,
                                automaticReadTracking);

                return new GrowingUpdateAlphaTransaction.Factory(config);
            }
        }
    }
}
