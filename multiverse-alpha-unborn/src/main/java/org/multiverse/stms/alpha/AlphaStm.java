package org.multiverse.stms.alpha;

import org.multiverse.annotations.LogLevel;
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

    private final LogLevel logLevel;

    private final PrimitiveClock clock;

    private final CommitLockPolicy commitLockPolicy;

    private final BackoffPolicy backoffPolicy;

    private final int maxRetries;

    private final int maxArraySize;

    private final boolean speculativeConfigEnabled;

    private final boolean optimizeConflictDetectionEnabled;

    private final boolean dirtyCheckEnabled;

    private final boolean quickReleaseWriteLocksEnabled;

    private final boolean explicitRetryAllowed;

    private final boolean readTrackingEnabled;

    private final boolean allowWriteSkew;

    private final boolean interruptible;

    private final AlphaProgrammaticReferenceFactoryBuilder referenceFactoryBuilder;

    private final int maxReadSpinCount;

    private final boolean loggingOfControlFlowErrorsEnabled;

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
        this.maxRetries = config.maxRetries;
        this.clock = config.clock;
        this.quickReleaseWriteLocksEnabled = config.quickReleaseWriteLocksEnabled;
        this.referenceFactoryBuilder = new AlphaProgrammaticReferenceFactoryBuilder(this);
        this.explicitRetryAllowed = config.explicitRetryAllowed;
        this.readTrackingEnabled = config.readTrackingEnabled;
        this.allowWriteSkew = config.allowWriteSkew;
        this.interruptible = config.interruptible;
        this.maxReadSpinCount = config.maxReadSpinCount;
        this.loggingOfControlFlowErrorsEnabled = config.loggingOfControlFlowErrorsEnabled;
        this.logLevel = config.logLevel;

        if (clock.getVersion() == 0) {
            clock.tick();
        }

        logger.info("Created a new AlphaStm instance");
    }

    @Override
    public AlphaTransactionFactoryBuilder getTransactionFactoryBuilder() {
        return new AlphaTransactionFactoryBuilder();
    }

    public int getMaxReadSpinCount() {
        return maxReadSpinCount;
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

    public int getMaxRetries() {
        return maxRetries;
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

        private final int maxRetries;
        private final boolean readonly;
        private final String familyName;
        private final boolean readTrackingEnabled;
        private final boolean writeSkewAllowed;
        private final CommitLockPolicy commitLockPolicy;
        private final BackoffPolicy backoffPolicy;
        private final SpeculativeConfiguration speculativeConfig;
        private final boolean interruptible;
        private final boolean dirtyCheck;
        private final boolean quickReleaseEnabled;
        private final boolean explicitRetryAllowed;
        private final long timeoutNs;
        private final int maxReadSpinCount;
        private final LogLevel logLevel;

        @Override
        public AlphaStm getStm() {
            return AlphaStm.this;
        }

        public AlphaTransactionFactoryBuilder() {
            this(false, //readonly
                    AlphaStm.this.readTrackingEnabled,
                    createAnonymousFamilyName(),
                    AlphaStm.this.maxRetries,
                    AlphaStm.this.allowWriteSkew,
                    AlphaStm.this.commitLockPolicy,
                    AlphaStm.this.backoffPolicy,
                    createSpeculativeConfiguration(speculativeConfigEnabled, maxArraySize),
                    AlphaStm.this.interruptible,
                    AlphaStm.this.dirtyCheckEnabled,
                    AlphaStm.this.quickReleaseWriteLocksEnabled,
                    AlphaStm.this.explicitRetryAllowed,
                    Long.MAX_VALUE,
                    AlphaStm.this.maxReadSpinCount,
                    AlphaStm.this.logLevel);
        }

        public AlphaTransactionFactoryBuilder(
                boolean readonly, boolean readTrackingEnabled, String familyName,
                int maxRetries, boolean writeSkewAllowed,
                CommitLockPolicy commitLockPolicy, BackoffPolicy backoffPolicy,
                SpeculativeConfiguration speculativeConfig, boolean interruptible,
                boolean dirtyCheck, boolean quickReleaseEnabled,
                boolean explicitRetryAllowed, long timeoutNs, int maxReadSpinCount, LogLevel logLevel) {
            this.readonly = readonly;
            this.familyName = familyName;
            this.maxRetries = maxRetries;
            this.readTrackingEnabled = readTrackingEnabled;
            this.writeSkewAllowed = writeSkewAllowed;
            this.commitLockPolicy = commitLockPolicy;
            this.backoffPolicy = backoffPolicy;
            this.speculativeConfig = speculativeConfig;
            this.interruptible = interruptible;
            this.dirtyCheck = dirtyCheck;
            this.quickReleaseEnabled = quickReleaseEnabled;
            this.explicitRetryAllowed = explicitRetryAllowed;
            this.timeoutNs = timeoutNs;
            this.maxReadSpinCount = maxReadSpinCount;
            this.logLevel = logLevel;
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
        public boolean isReadTrackingEnabled() {
            return readTrackingEnabled;
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
        public boolean isWriteSkewAllowed() {
            return writeSkewAllowed;
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
        public int getMaxRetries() {
            return maxRetries;
        }

        @Override
        public LogLevel getLogLevel() {
            return logLevel;
        }

        @Override
        public AlphaTransactionFactoryBuilder setLogLevel(LogLevel logLevel) {
            if (logLevel == null) {
                throw new NullPointerException();
            }

            return new AlphaTransactionFactoryBuilder(
                    readonly, readTrackingEnabled, familyName, maxRetries,
                    writeSkewAllowed, commitLockPolicy, backoffPolicy,
                    speculativeConfig, interruptible, dirtyCheck, quickReleaseEnabled,
                    explicitRetryAllowed, timeoutNs, maxReadSpinCount, logLevel);
        }

        @Override
        public int getMaxReadSpinCount() {
            return maxReadSpinCount;
        }

        @Override
        public AlphaTransactionFactoryBuilder setMaxReadSpinCount(int maxReadSpinCount) {
            if (maxReadSpinCount < 0) {
                throw new IllegalArgumentException();
            }

            return new AlphaTransactionFactoryBuilder(
                    readonly, readTrackingEnabled, familyName, maxRetries,
                    writeSkewAllowed, commitLockPolicy, backoffPolicy,
                    speculativeConfig, interruptible, dirtyCheck, quickReleaseEnabled,
                    explicitRetryAllowed, timeoutNs, maxReadSpinCount, logLevel);
        }

        @Override
        public AlphaTransactionFactoryBuilder setTimeoutNs(long timeoutNs) {
            if (timeoutNs < 0) {
                throw new IllegalArgumentException();
            }

            return new AlphaTransactionFactoryBuilder(
                    readonly, readTrackingEnabled, familyName, maxRetries,
                    writeSkewAllowed, commitLockPolicy, backoffPolicy,
                    speculativeConfig, interruptible, dirtyCheck, quickReleaseEnabled,
                    explicitRetryAllowed, timeoutNs, maxReadSpinCount, logLevel);
        }

        @Override
        public AlphaTransactionFactoryBuilder setFamilyName(String familyName) {
            return new AlphaTransactionFactoryBuilder(
                    readonly, readTrackingEnabled, familyName, maxRetries,
                    writeSkewAllowed, commitLockPolicy, backoffPolicy,
                    speculativeConfig, interruptible, dirtyCheck, quickReleaseEnabled,
                    explicitRetryAllowed, timeoutNs, maxReadSpinCount, logLevel);
        }

        @Override
        public AlphaTransactionFactoryBuilder setQuickReleaseEnabled(boolean quickReleaseEnabled) {
            return new AlphaTransactionFactoryBuilder(
                    readonly, readTrackingEnabled, familyName, maxRetries,
                    writeSkewAllowed, commitLockPolicy, backoffPolicy,
                    speculativeConfig, interruptible, dirtyCheck, quickReleaseEnabled,
                    explicitRetryAllowed, timeoutNs, maxReadSpinCount, logLevel);
        }

        @Override
        public AlphaTransactionFactoryBuilder setMaxRetries(int maxRetries) {
            if (maxRetries < 0) {
                throw new IllegalArgumentException(format("retryCount can't be smaller than 0, found %s", maxRetries));
            }

            return new AlphaTransactionFactoryBuilder(
                    readonly, readTrackingEnabled, familyName, maxRetries,
                    writeSkewAllowed, commitLockPolicy, backoffPolicy,
                    speculativeConfig, interruptible, dirtyCheck, quickReleaseEnabled,
                    explicitRetryAllowed, timeoutNs, maxReadSpinCount, logLevel);
        }

        @Override
        public AlphaTransactionFactoryBuilder setReadonly(boolean readonly) {
            SpeculativeConfiguration newSpeculativeConfig = speculativeConfig.withSpeculativeReadonlyDisabled();

            return new AlphaTransactionFactoryBuilder(
                    readonly, readTrackingEnabled, familyName, maxRetries,
                    writeSkewAllowed, commitLockPolicy, backoffPolicy,
                    newSpeculativeConfig, interruptible, dirtyCheck, quickReleaseEnabled,
                    explicitRetryAllowed, timeoutNs, maxReadSpinCount, logLevel);
        }

        public AlphaTransactionFactoryBuilder setReadTrackingEnabled(boolean readTrackingEnabled) {
            SpeculativeConfiguration newSpeculativeConfig = speculativeConfig.withSpeculativeNonAutomaticReadTrackingDisabled();

            return new AlphaTransactionFactoryBuilder(
                    readonly, readTrackingEnabled, familyName, maxRetries,
                    writeSkewAllowed, commitLockPolicy, backoffPolicy,
                    newSpeculativeConfig, interruptible, dirtyCheck, quickReleaseEnabled,
                    explicitRetryAllowed, timeoutNs, maxReadSpinCount, logLevel);
        }

        @Override
        public AlphaTransactionFactoryBuilder setInterruptible(boolean interruptible) {
            return new AlphaTransactionFactoryBuilder(
                    readonly, readTrackingEnabled, familyName, maxRetries,
                    writeSkewAllowed, commitLockPolicy, backoffPolicy,
                    speculativeConfig, interruptible, dirtyCheck, quickReleaseEnabled,
                    explicitRetryAllowed, timeoutNs, maxReadSpinCount, logLevel);
        }

        @Override
        public AlphaTransactionFactoryBuilder setCommitLockPolicy(CommitLockPolicy commitLockPolicy) {
            if (commitLockPolicy == null) {
                throw new NullPointerException();
            }

            return new AlphaTransactionFactoryBuilder(
                    readonly, readTrackingEnabled, familyName, maxRetries,
                    writeSkewAllowed, commitLockPolicy, backoffPolicy,
                    speculativeConfig, interruptible, dirtyCheck, quickReleaseEnabled,
                    explicitRetryAllowed, timeoutNs, maxReadSpinCount, logLevel);
        }

        @Override
        public AlphaTransactionFactoryBuilder setSpeculativeConfigurationEnabled(boolean enabled) {
            SpeculativeConfiguration newSpeculativeConfig = createSpeculativeConfiguration(enabled, maxArraySize);

            return new AlphaTransactionFactoryBuilder(
                    readonly, readTrackingEnabled, familyName, maxRetries,
                    writeSkewAllowed, commitLockPolicy, backoffPolicy, newSpeculativeConfig,
                    interruptible, dirtyCheck, quickReleaseEnabled, explicitRetryAllowed,
                    timeoutNs, maxReadSpinCount, logLevel);
        }

        @Override
        public AlphaTransactionFactoryBuilder setWriteSkewAllowed(boolean allowWriteSkew) {
            return new AlphaTransactionFactoryBuilder(
                    readonly, readTrackingEnabled, familyName, maxRetries,
                    allowWriteSkew, commitLockPolicy, backoffPolicy, speculativeConfig,
                    interruptible, dirtyCheck, quickReleaseEnabled, explicitRetryAllowed,
                    timeoutNs, maxReadSpinCount, logLevel);
        }

        @Override
        public AlphaTransactionFactoryBuilder setBackoffPolicy(BackoffPolicy backoffPolicy) {
            if (backoffPolicy == null) {
                throw new NullPointerException();
            }

            return new AlphaTransactionFactoryBuilder(
                    readonly, readTrackingEnabled, familyName, maxRetries,
                    writeSkewAllowed, commitLockPolicy, backoffPolicy,
                    speculativeConfig, interruptible, dirtyCheck, quickReleaseEnabled,
                    explicitRetryAllowed, timeoutNs, maxReadSpinCount, logLevel);
        }

        @Override
        public AlphaTransactionFactoryBuilder setDirtyCheckEnabled(boolean dirtyCheckEnabled) {
            return new AlphaTransactionFactoryBuilder(
                    readonly, readTrackingEnabled, familyName, maxRetries,
                    writeSkewAllowed, commitLockPolicy, backoffPolicy,
                    speculativeConfig, interruptible, dirtyCheckEnabled, quickReleaseEnabled,
                    explicitRetryAllowed, timeoutNs, maxReadSpinCount, logLevel);
        }

        @Override
        public AlphaTransactionFactoryBuilder setExplicitRetryAllowed(boolean explicitRetryAllowed) {
            return new AlphaTransactionFactoryBuilder(
                    readonly, readTrackingEnabled, familyName, maxRetries,
                    writeSkewAllowed, commitLockPolicy, backoffPolicy,
                    speculativeConfig, interruptible, dirtyCheck, quickReleaseEnabled,
                    explicitRetryAllowed, timeoutNs, maxReadSpinCount, logLevel);
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
            return new TransactionFactory<AlphaTransaction>() {

                final ReadonlyConfiguration ro_nort =
                        new ReadonlyConfiguration(
                                clock, backoffPolicy, familyName, speculativeConfig, maxRetries,
                                interruptible, false, explicitRetryAllowed, timeoutNs, maxReadSpinCount,
                                this, logLevel);
                final ReadonlyConfiguration ro_rt =
                        new ReadonlyConfiguration(
                                clock, backoffPolicy, familyName, speculativeConfig, maxRetries,
                                interruptible, true, explicitRetryAllowed, timeoutNs, maxReadSpinCount,
                                this, logLevel);
                final UpdateConfiguration up_rt =
                        new UpdateConfiguration(
                                clock, backoffPolicy, commitLockPolicy, familyName, speculativeConfig,
                                maxRetries, interruptible, true, writeSkewAllowed,
                                optimizeConflictDetectionEnabled, true, quickReleaseEnabled,
                                explicitRetryAllowed, timeoutNs, maxReadSpinCount, this, logLevel);
                final UpdateConfiguration up_nort =
                        new UpdateConfiguration(
                                clock, backoffPolicy, commitLockPolicy, familyName,
                                speculativeConfig, maxRetries, interruptible, false, true,
                                optimizeConflictDetectionEnabled, true, quickReleaseEnabled,
                                explicitRetryAllowed, timeoutNs, maxReadSpinCount, this, logLevel);

                @Override
                public Stm getStm() {
                    return AlphaStm.this;
                }

                @Override
                public TransactionFactoryBuilder getTransactionFactoryBuilder() {
                    return AlphaTransactionFactoryBuilder.this;
                }

                @Override
                public AlphaTransaction start() {
                    boolean finalReadonly;
                    if (speculativeConfig.isSpeculativeReadonlyEnabled()) {
                        finalReadonly = speculativeConfig.isReadonly();
                    } else {
                        finalReadonly = readonly;
                    }

                    boolean finalAutomaticReadTracking;
                    if (speculativeConfig.isSpeculativeNoReadTrackingEnabled()) {
                        finalAutomaticReadTracking = speculativeConfig.isReadTrackingEnabled();
                    } else {
                        finalAutomaticReadTracking = readTrackingEnabled;
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
            return new TransactionFactory<AlphaTransaction>() {
                ReadonlyConfiguration config =
                        new ReadonlyConfiguration(
                                clock, backoffPolicy, familyName, speculativeConfig,
                                maxRetries, interruptible, readTrackingEnabled,
                                explicitRetryAllowed, timeoutNs, maxReadSpinCount,
                                this, logLevel);

                @Override
                public Stm getStm() {
                    return AlphaStm.this;
                }

                @Override
                public TransactionFactoryBuilder getTransactionFactoryBuilder() {
                    return AlphaTransactionFactoryBuilder.this;
                }

                @Override
                public AlphaTransaction start() {
                    if (readTrackingEnabled) {
                        return new MapReadonlyAlphaTransaction(config);
                    } else {
                        return new NonTrackingReadonlyAlphaTransaction(config);
                    }
                }
            };
        }

        private TransactionFactory<AlphaTransaction> createNonSpeculativeUpdateTxFactory() {
            if (!readTrackingEnabled && !writeSkewAllowed) {
                String msg = format("Can't createReference transactionfactory for transaction family '%s' because an update "
                        + "transaction without automaticReadTracking and with writeSkew disallowed is "
                        + "not possible", familyName
                );

                throw new IllegalStateException(msg);
            }

            return new TransactionFactory<AlphaTransaction>() {
                UpdateConfiguration config =
                        new UpdateConfiguration(
                                clock, backoffPolicy, commitLockPolicy, familyName, speculativeConfig,
                                maxRetries, interruptible, readTrackingEnabled, writeSkewAllowed,
                                optimizeConflictDetectionEnabled, true, quickReleaseEnabled,
                                explicitRetryAllowed, timeoutNs, maxReadSpinCount, this, logLevel);


                @Override
                public Stm getStm() {
                    return AlphaStm.this;
                }

                @Override
                public TransactionFactoryBuilder getTransactionFactoryBuilder() {
                    return AlphaTransactionFactoryBuilder.this;
                }

                @Override
                public AlphaTransaction start() {
                    return new MapUpdateAlphaTransaction(config);
                }
            };
        }
    }
}
