package org.multiverse.stms.alpha;

import org.multiverse.annotations.LogLevel;
import org.multiverse.api.backoff.BackoffPolicy;
import org.multiverse.api.backoff.ExponentialBackoffPolicy;
import org.multiverse.api.clock.PrimitiveClock;
import org.multiverse.api.clock.RelaxedPrimitiveClock;
import org.multiverse.api.clock.StrictPrimitiveClock;
import org.multiverse.api.commitlock.CommitLockPolicy;
import org.multiverse.api.commitlock.GenericCommitLockPolicy;

/**
 * An Object responsible for storing the configuration for an {@link AlphaStm}. So instead of having to deal with ever
 * growing number of arguments, the AlphaStm constructor only needs a single argument.
 * <p/>
 * Once the AlphaStm is constructed, changes made to the config object is not visible to the stm that received. It wil
 * have copied all fields, and doesn't read from the config after creation anymore.
 * <p/>
 * AlphaStmConfig is not thread-safe. It can be shared between threads as long as no changes are to the config and there
 * is a save handover point from construction to usage. A volatile variable or mutex would do the trick. As long as
 * there is a happens before relation between the write and the read expressed in terms of the JMM.
 * <p/>
 * A config will always be checked by the constructor of the stm if all fields are correctly initialized.
 *
 * @author Peter Veentjer.
 */
public final class AlphaStmConfig {
    public static AlphaStmConfig createDebugConfig() {
        AlphaStmConfig config = new AlphaStmConfig();
        config.speculativeConfigurationEnabled = false;
        config.optimizedConflictDetectionEnabled = false;
        config.quickReleaseWriteLocksEnabled = false;
        config.clock = new StrictPrimitiveClock();
        return config;
    }

    public static AlphaStmConfig createFastConfig() {
        AlphaStmConfig config = new AlphaStmConfig();
        config.optimizedConflictDetectionEnabled = true;
        config.speculativeConfigurationEnabled = true;
        config.quickReleaseWriteLocksEnabled = true;
        config.clock = new RelaxedPrimitiveClock();
        return config;
    }

    public int maxReadSpinCount = 50;

    public PrimitiveClock clock = new StrictPrimitiveClock();

    public CommitLockPolicy commitLockPolicy =
            GenericCommitLockPolicy.FAIL_FAST_BUT_RETRY;

    public BackoffPolicy backoffPolicy =
            ExponentialBackoffPolicy.INSTANCE_100_MS_MAX;

    public boolean quickReleaseWriteLocksEnabled = true;

    public boolean speculativeConfigurationEnabled = false;

    public int maxRetries = 1000;

    public int maxFixedUpdateSize = 20;

    public boolean optimizedConflictDetectionEnabled = true;

    public boolean dirtyCheckEnabled = true;

    public boolean explicitRetryAllowed = true;

    public boolean readTrackingEnabled = false;

    public boolean allowWriteSkew = true;

    public boolean interruptible = false;

    public boolean loggingOfControlFlowErrorsEnabled = false;

    public LogLevel logLevel = LogLevel.none;

    /**
     * Check if the AlphaStmConfig has been configured correctly.
     *
     * @throws IllegalStateException if the configuration isn't valid.
     */
    public void ensureValid() {
        if (maxRetries < 0) {
            throw new IllegalStateException("Invalid configuration, 'maxRetries' can't be smaller than 0");
        }

        if (commitLockPolicy == null) {
            throw new IllegalStateException("Invalid configuration, 'commitLockPolicy' can't be null");
        }

        if (clock == null) {
            throw new IllegalStateException("Invalid configuration, 'clock' can't be null");
        }

        if (backoffPolicy == null) {
            throw new IllegalStateException("Invalid configuration, 'backoffPolicy' can't be null");
        }

        if (maxFixedUpdateSize < 0) {
            throw new IllegalStateException("Invalid configuration, fixedLengthMaximumSize can't be smaller than 0");
        }

        if (maxRetries < 0) {
            throw new IllegalStateException("Invalid configuration, defaultMaxRetryCount can't be smaller than 0");
        }

        if (maxReadSpinCount < 0) {
            throw new IllegalStateException("Invalid configuration, maxReadSprinCount can't be smaller than 0");
        }

        if (logLevel == null) {
            throw new IllegalStateException("Invalid configuration, logLevel can't be null");
        }
    }
}
