package org.multiverse.stms.alpha;

import org.multiverse.utils.backoff.BackoffPolicy;
import org.multiverse.utils.backoff.ExponentialBackoffPolicy;
import org.multiverse.utils.clock.PrimitiveClock;
import org.multiverse.utils.clock.RelaxedPrimitiveClock;
import org.multiverse.utils.clock.StrictPrimitiveClock;
import org.multiverse.utils.commitlock.CommitLockPolicy;
import org.multiverse.utils.commitlock.GenericCommitLockPolicy;

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
        config.clock = new StrictPrimitiveClock();
        return config;
    }

    public static AlphaStmConfig createFastConfig() {
        AlphaStmConfig config = new AlphaStmConfig();
        config.optimizedConflictDetectionEnabled = true;
        config.speculativeConfigurationEnabled = true;
        config.clock = new RelaxedPrimitiveClock();
        return config;
    }

    public PrimitiveClock clock = new StrictPrimitiveClock();

    public CommitLockPolicy commitLockPolicy =
            GenericCommitLockPolicy.FAIL_FAST_BUT_RETRY;

    public BackoffPolicy backoffPolicy =
            ExponentialBackoffPolicy.INSTANCE_10_MS_MAX;

    public boolean speculativeConfigurationEnabled = false;

    public int maxRetryCount = 1000;

    public int maxFixedUpdateSize = 20;

    public boolean optimizedConflictDetectionEnabled = true;

    public boolean dirtyCheckEnabled = true;


    /**
     * Check if the AlphaStmConfig has been configured correctly.
     *
     * @throws IllegalStateException if the configuration isn't valid.
     */
    public void ensureValid() {
        if (maxRetryCount < 0) {
            throw new IllegalStateException("Invalid configuration, 'maxRetryCount' can't be smaller than 0");
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

        if (maxRetryCount < 0) {
            throw new IllegalStateException("Invalid configuration, defaultMaxRetryCount can't be smaller than 0");
        }
    }
}
