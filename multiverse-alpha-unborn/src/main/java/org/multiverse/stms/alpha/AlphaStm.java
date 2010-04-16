package org.multiverse.stms.alpha;

import org.multiverse.api.Stm;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.TransactionFactoryBuilder;
import org.multiverse.api.backoff.BackoffPolicy;
import org.multiverse.api.clock.PrimitiveClock;
import org.multiverse.api.commitlock.CommitLockPolicy;
import org.multiverse.stms.alpha.programmatic.AlphaProgrammaticReferenceFactoryBuilder;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.stms.alpha.transactions.SpeculativeConfiguration;
import org.multiverse.stms.alpha.transactions.readonly.*;
import org.multiverse.stms.alpha.transactions.update.ArrayUpdateAlphaTransaction;
import org.multiverse.stms.alpha.transactions.update.MapUpdateAlphaTransaction;
import org.multiverse.stms.alpha.transactions.update.MonoUpdateAlphaTransaction;
import org.multiverse.stms.alpha.transactions.update.UpdateConfiguration;

import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import static java.lang.String.format;
import static org.multiverse.stms.alpha.transactions.SpeculativeConfiguration.createSpeculativeConfiguration;

/**
 * Default {@link Stm} implementation that provides the most complete set of features. Like retry/orelse.
 * <p/>
 * It can be configured through the {@link AlphaStmConfig}.
 *
 * @author Peter Veentjer.
 */
public final class AlphaStm implements Stm<AlphaStm.AlphaTransactionFactoryBuilder, AlphaProgrammaticReferenceFactoryBuilder> {

    private final static Logger logger = Logger.getLogger(AlphaStm.class.getName());

    private final static AtomicLong anonoymousFamilyNameGenerator = new AtomicLong();

    private static String createAnonymousFamilyName() {
        return "TransactionFamily-" + anonoymousFamilyNameGenerator.incrementAndGet();
    }

    private final PrimitiveClock clock;

    private final CommitLockPolicy commitLockPolicy;

    private final BackoffPolicy backoffPolicy;

    private final int maxRetryCount;

    private final int maxArraySize;

    private final boolean speculativeConfigEnabled;

    private final boolean optimizeConflictDetectionEnabled;

    private final boolean dirtyCheckEnabled;

    private final boolean quickReleaseWriteLocksEnabled;

    private final boolean explicitRetryAllowed;

    private final boolean automaticReadTracking;

    private final boolean allowWriteSkewProblem;

    private final boolean interruptible;

    private final AlphaProgrammaticReferenceFactoryBuilder referenceFactoryBuilder;

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

        this.speculativeConfigEnabled = config.speculativeConfigurationEnabled;
        this.optimizeConflictDetectionEnabled = config.optimizedConflictDetectionEnabled;
        this.dirtyCheckEnabled = config.dirtyCheckEnabled;
        this.maxArraySize = config.maxFixedUpdateSize;
        this.commitLockPolicy = config.commitLockPolicy;
        this.backoffPolicy = config.backoffPolicy;
        this.maxRetryCount = config.maxRetryCount;
        this.clock = config.clock;
        this.quickReleaseWriteLocksEnabled = config.quickReleaseWriteLocksEnabled;
        this.referenceFactoryBuilder = new AlphaProgrammaticReferenceFactoryBuilder(this);
        this.explicitRetryAllowed = config.explicitRetryAllowed;
        this.automaticReadTracking = config.automaticReadTrackingEnabled;
        this.allowWriteSkewProblem = config.allowWriteSkewProblem;
        this.interruptible = config.interruptible;

        if (clock.getVersion() == 0) {
            clock.tick();
        }

        logger.info("Created a new AlphaStm instance");
    }

    @Override
    public AlphaTransactionFactoryBuilder getTransactionFactoryBuilder() {
        return new AlphaTransactionFactoryBuilder();
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

    public CommitLockPolicy getCommitLockPolicy() {
        return commitLockPolicy;
    }

    public boolean isDirtyCheckEnabled() {
        return dirtyCheckEnabled;
    }

    public int getMaxArraySize() {
        return maxArraySize;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public boolean isOptimizeConflictDetectionEnabled() {
        return optimizeConflictDetectionEnabled;
    }

    public boolean isQuickReleaseWriteLocksEnabled() {
        return quickReleaseWriteLocksEnabled;
    }

    public boolean isSpeculativeConfigEnabled() {
        return speculativeConfigEnabled;
    }

    @Override
    public long getVersion() {
        return clock.getVersion();
    }

    public PrimitiveClock getClock() {
        return clock;
    }

    @Override
    public AlphaProgrammaticReferenceFactoryBuilder getProgrammaticReferenceFactoryBuilder() {
        return referenceFactoryBuilder;
    }

    public class AlphaTransactionFactoryBuilder
            implements TransactionFactoryBuilder<AlphaTransaction, AlphaTransactionFactoryBuilder> {

        private final int maxRetryCount;
        private final boolean readonly;
        private final String familyName;
        private final boolean automaticReadTracking;
        private final boolean writeSkewProblemAllowed;
        private final CommitLockPolicy commitLockPolicy;
        private final BackoffPolicy backoffPolicy;
        private final SpeculativeConfiguration speculativeConfig;
        private final boolean interruptible;
        private final boolean dirtyCheck;
        private final boolean quickReleaseEnabled;
        private final boolean explicitRetryAllowed;
        private final long timeoutNs;

        public AlphaTransactionFactoryBuilder() {
            this(false, //readonly
                    AlphaStm.this.automaticReadTracking,
                    createAnonymousFamilyName(),
                    AlphaStm.this.maxRetryCount,
                    AlphaStm.this.allowWriteSkewProblem,
                    AlphaStm.this.commitLockPolicy,
                    AlphaStm.this.backoffPolicy,
                    createSpeculativeConfiguration(speculativeConfigEnabled, maxArraySize),
                    AlphaStm.this.interruptible,
                    AlphaStm.this.dirtyCheckEnabled,
                    AlphaStm.this.quickReleaseWriteLocksEnabled,
                    AlphaStm.this.explicitRetryAllowed,
                    Long.MAX_VALUE);
        }

        public AlphaTransactionFactoryBuilder(
                boolean readonly, boolean automaticReadTracking, String familyName,
                int maxRetryCount, boolean writeSkewProblemAllowed,
                CommitLockPolicy commitLockPolicy, BackoffPolicy backoffPolicy,
                SpeculativeConfiguration speculativeConfig, boolean interruptible,
                boolean dirtyCheck, boolean quickReleaseEnabled,
                boolean explicitRetryAllowed, long timeoutNs) {
            this.readonly = readonly;
            this.familyName = familyName;
            this.maxRetryCount = maxRetryCount;
            this.automaticReadTracking = automaticReadTracking;
            this.writeSkewProblemAllowed = writeSkewProblemAllowed;
            this.commitLockPolicy = commitLockPolicy;
            this.backoffPolicy = backoffPolicy;
            this.speculativeConfig = speculativeConfig;
            this.interruptible = interruptible;
            this.dirtyCheck = dirtyCheck;
            this.quickReleaseEnabled = quickReleaseEnabled;
            this.explicitRetryAllowed = explicitRetryAllowed;
            this.timeoutNs = timeoutNs;
        }

        @Override
        public BackoffPolicy getBackoffPolicy() {
            return backoffPolicy;
        }

        @Override
        public boolean isDirtyCheckEnabled() {
            return dirtyCheckEnabled;
        }

        @Override
        public boolean isExplicitRetryAllowed() {
            return explicitRetryAllowed;
        }

        @Override
        public String getFamilyName() {
            return familyName;
        }

        @Override
        public boolean isReadonly() {
            return readonly;
        }

        @Override
        public boolean isAutomaticReadTracking() {
            return automaticReadTracking;
        }

        @Override
        public boolean isInterruptible() {
            return interruptible;
        }

        @Override
        public CommitLockPolicy getCommitLockPolicy() {
            return commitLockPolicy;
        }

        @Override
        public boolean isSpeculativeConfigurationEnabled() {
            return speculativeConfigEnabled;
        }

        @Override
        public boolean isWriteSkewProblemAllowed() {
            return writeSkewProblemAllowed;
        }

        @Override
        public boolean isQuickReleaseEnabled() {
            return quickReleaseEnabled;
        }

        @Override
        public long getTimeoutNs() {
            return timeoutNs;
        }

        @Override
        public int getMaxRetryCount() {
            return maxRetryCount;
        }

        @Override
        public AlphaTransactionFactoryBuilder setTimeoutNs(long timeoutNs) {
            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, maxRetryCount,
                    writeSkewProblemAllowed, commitLockPolicy, backoffPolicy,
                    speculativeConfig, interruptible, dirtyCheck, quickReleaseEnabled,
                    explicitRetryAllowed, timeoutNs);
        }

        @Override
        public AlphaTransactionFactoryBuilder setFamilyName(String familyName) {
            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, maxRetryCount,
                    writeSkewProblemAllowed, commitLockPolicy, backoffPolicy,
                    speculativeConfig, interruptible, dirtyCheck, quickReleaseEnabled,
                    explicitRetryAllowed, timeoutNs);
        }

        @Override
        public AlphaTransactionFactoryBuilder setQuickReleaseEnabled(boolean quickReleaseEnabled) {
            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, maxRetryCount,
                    writeSkewProblemAllowed, commitLockPolicy, backoffPolicy,
                    speculativeConfig, interruptible, dirtyCheck, quickReleaseEnabled,
                    explicitRetryAllowed, timeoutNs);
        }

        @Override
        public AlphaTransactionFactoryBuilder setMaxRetryCount(int maxRetryCount) {
            if (maxRetryCount < 0) {
                throw new IllegalArgumentException(format("retryCount can't be smaller than 0, found %s", maxRetryCount));
            }

            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, maxRetryCount,
                    writeSkewProblemAllowed, commitLockPolicy, backoffPolicy,
                    speculativeConfig, interruptible, dirtyCheck, quickReleaseEnabled,
                    explicitRetryAllowed, timeoutNs);
        }

        @Override
        public AlphaTransactionFactoryBuilder setReadonly(boolean readonly) {
            SpeculativeConfiguration newSpeculativeConfig = speculativeConfig.withSpeculativeReadonlyDisabled();

            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, maxRetryCount,
                    writeSkewProblemAllowed, commitLockPolicy, backoffPolicy,
                    newSpeculativeConfig, interruptible, dirtyCheck, quickReleaseEnabled,
                    explicitRetryAllowed, timeoutNs);
        }

        public AlphaTransactionFactoryBuilder setAutomaticReadTrackingEnabled(boolean automaticReadTrackingEnabled) {
            SpeculativeConfiguration newSpeculativeConfig = speculativeConfig.withSpeculativeNonAutomaticReadTrackingDisabled();

            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTrackingEnabled, familyName, maxRetryCount,
                    writeSkewProblemAllowed, commitLockPolicy, backoffPolicy,
                    newSpeculativeConfig, interruptible, dirtyCheck, quickReleaseEnabled,
                    explicitRetryAllowed, timeoutNs);
        }

        @Override
        public AlphaTransactionFactoryBuilder setInterruptible(boolean interruptible) {
            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, maxRetryCount,
                    writeSkewProblemAllowed, commitLockPolicy, backoffPolicy,
                    speculativeConfig, interruptible, dirtyCheck, quickReleaseEnabled,
                    explicitRetryAllowed, timeoutNs);
        }

        @Override
        public AlphaTransactionFactoryBuilder setCommitLockPolicy(CommitLockPolicy commitLockPolicy) {
            if (commitLockPolicy == null) {
                throw new NullPointerException();
            }

            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, maxRetryCount,
                    writeSkewProblemAllowed, commitLockPolicy, backoffPolicy,
                    speculativeConfig, interruptible, dirtyCheck, quickReleaseEnabled,
                    explicitRetryAllowed, timeoutNs);
        }

        @Override
        public AlphaTransactionFactoryBuilder setSpeculativeConfigurationEnabled(boolean enabled) {
            SpeculativeConfiguration newSpeculativeConfig = createSpeculativeConfiguration(enabled, maxArraySize);

            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, maxRetryCount,
                    writeSkewProblemAllowed, commitLockPolicy, backoffPolicy, newSpeculativeConfig,
                    interruptible, dirtyCheck, quickReleaseEnabled, explicitRetryAllowed, timeoutNs);
        }

        @Override
        public AlphaTransactionFactoryBuilder setWriteSkewProblemAllowed(boolean allowWriteSkew) {
            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, maxRetryCount,
                    allowWriteSkew, commitLockPolicy, backoffPolicy, speculativeConfig,
                    interruptible, dirtyCheck, quickReleaseEnabled, explicitRetryAllowed, timeoutNs);
        }

        @Override
        public AlphaTransactionFactoryBuilder setBackoffPolicy(BackoffPolicy backoffPolicy) {
            if (backoffPolicy == null) {
                throw new NullPointerException();
            }

            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, maxRetryCount,
                    writeSkewProblemAllowed, commitLockPolicy, backoffPolicy,
                    speculativeConfig, interruptible, dirtyCheck, quickReleaseEnabled,
                    explicitRetryAllowed, timeoutNs);
        }

        @Override
        public AlphaTransactionFactoryBuilder setDirtyCheckEnabled(boolean dirtyCheckEnabled) {
            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, maxRetryCount,
                    writeSkewProblemAllowed, commitLockPolicy, backoffPolicy,
                    speculativeConfig, interruptible, dirtyCheckEnabled, quickReleaseEnabled,
                    explicitRetryAllowed, timeoutNs);
        }

        @Override
        public AlphaTransactionFactoryBuilder setExplicitRetryAllowed(boolean explicitRetryAllowed) {
            return new AlphaTransactionFactoryBuilder(
                    readonly, automaticReadTracking, familyName, maxRetryCount,
                    writeSkewProblemAllowed, commitLockPolicy, backoffPolicy,
                    speculativeConfig, interruptible, dirtyCheck, quickReleaseEnabled,
                    explicitRetryAllowed, timeoutNs);
        }

        @Override
        public TransactionFactory<AlphaTransaction> build() {
            if (speculativeConfig.isEnabled()) {
                return createSpeculativeTxFactory();
            } else if (readonly) {
                return createNonSpeculativeReadonlyTxFactory();
            } else {
                return createNonSpeculativeUpdateTxFactory();
            }
        }

        private TransactionFactory<AlphaTransaction> createSpeculativeTxFactory() {
            final ReadonlyConfiguration ro_nort =
                    new ReadonlyConfiguration(
                            clock, backoffPolicy, familyName, speculativeConfig, maxRetryCount,
                            interruptible, false, explicitRetryAllowed, timeoutNs);
            final ReadonlyConfiguration ro_rt =
                    new ReadonlyConfiguration(
                            clock, backoffPolicy, familyName, speculativeConfig, maxRetryCount,
                            interruptible, true, explicitRetryAllowed, timeoutNs);
            final UpdateConfiguration up_rt =
                    new UpdateConfiguration(
                            clock, backoffPolicy, commitLockPolicy, familyName, speculativeConfig,
                            maxRetryCount, interruptible, true, writeSkewProblemAllowed,
                            optimizeConflictDetectionEnabled, true, quickReleaseEnabled,
                            explicitRetryAllowed, timeoutNs);
            final UpdateConfiguration up_nort =
                    new UpdateConfiguration(
                            clock, backoffPolicy, commitLockPolicy, familyName,
                            speculativeConfig, maxRetryCount, interruptible, false, true,
                            optimizeConflictDetectionEnabled, true, quickReleaseEnabled,
                            explicitRetryAllowed, timeoutNs);

            return new TransactionFactory<AlphaTransaction>() {
                @Override
                public AlphaTransaction start() {
                    boolean finalReadonly;
                    if (speculativeConfig.isSpeculativeReadonlyEnabled()) {
                        finalReadonly = speculativeConfig.isReadonly();
                    } else {
                        finalReadonly = readonly;
                    }

                    boolean finalAutomaticReadTracking;
                    if (speculativeConfig.isSpeculativeNonAutomaticReadTrackingEnabled()) {
                        finalAutomaticReadTracking = speculativeConfig.isAutomaticReadTracking();
                    } else {
                        finalAutomaticReadTracking = automaticReadTracking;
                    }

                    boolean speculativeSizeEnabled = speculativeConfig.isSpeculativeSizeEnabled();

                    if (finalReadonly) {
                        if (finalAutomaticReadTracking) {
                            if (speculativeSizeEnabled) {
                                int size = speculativeConfig.getOptimalSize();

                                if (size <= 1) {
                                    return new MonoReadonlyAlphaTransaction(ro_rt);
                                } else if (size < maxArraySize) {
                                    return new ArrayReadonlyAlphaTransaction(ro_rt, size);
                                } else {
                                    return new MapReadonlyAlphaTransaction(ro_rt);
                                }
                            } else {
                                return new MapReadonlyAlphaTransaction(ro_rt);
                            }
                        } else {
                            return new NonTrackingReadonlyAlphaTransaction(ro_nort);
                        }
                    } else {

                        UpdateConfiguration config;
                        if (finalAutomaticReadTracking) {
                            config = up_rt;
                        } else {
                            config = up_nort;
                        }

                        if (speculativeSizeEnabled) {
                            int size = speculativeConfig.getOptimalSize();

                            if (size <= 1) {
                                return new MonoUpdateAlphaTransaction(config);
                            } else if (size <= maxArraySize) {
                                return new ArrayUpdateAlphaTransaction(config, size);
                            } else {
                                return new MapUpdateAlphaTransaction(config);
                            }
                        } else {
                            return new MapUpdateAlphaTransaction(config);
                        }
                    }
                }
            };
        }

        private TransactionFactory<AlphaTransaction> createNonSpeculativeReadonlyTxFactory() {
            ReadonlyConfiguration config =
                    new ReadonlyConfiguration(
                            clock, backoffPolicy, familyName, speculativeConfig,
                            maxRetryCount, interruptible, automaticReadTracking,
                            explicitRetryAllowed, timeoutNs);

            if (automaticReadTracking) {
                return new MapReadonlyAlphaTransaction.Factory(config);
            } else {
                return new NonTrackingReadonlyAlphaTransaction.Factory(config);
            }
        }

        private TransactionFactory<AlphaTransaction> createNonSpeculativeUpdateTxFactory() {
            if (!automaticReadTracking && !writeSkewProblemAllowed) {
                String msg = format("Can't createReference transactionfactory for transaction family '%s' because an update "
                        + "transaction without automaticReadTracking and without isWriteSkewProblemAllowed is "
                        + "not possible", familyName
                );

                throw new IllegalStateException(msg);
            }

            UpdateConfiguration config =
                    new UpdateConfiguration(
                            clock, backoffPolicy, commitLockPolicy, familyName, speculativeConfig,
                            maxRetryCount, interruptible, automaticReadTracking, writeSkewProblemAllowed,
                            optimizeConflictDetectionEnabled, true, quickReleaseEnabled,
                            explicitRetryAllowed, timeoutNs);

            return new MapUpdateAlphaTransaction.Factory(config);
        }
    }
}
