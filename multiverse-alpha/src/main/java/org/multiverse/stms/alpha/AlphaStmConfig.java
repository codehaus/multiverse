package org.multiverse.stms.alpha;

import org.multiverse.utils.clock.Clock;
import org.multiverse.utils.clock.RelaxedClock;
import org.multiverse.utils.clock.StrictClock;
import org.multiverse.utils.commitlock.CommitLockPolicy;
import org.multiverse.utils.commitlock.GenericCommitLockPolicy;
import org.multiverse.utils.profiling.ProfileRepository;
import org.multiverse.utils.profiling.SimpleProfileRepository;
import org.multiverse.utils.restartbackoff.ExponentialRestartBackoffPolicy;
import org.multiverse.utils.restartbackoff.RestartBackoffPolicy;

/**
 * An Object responsible for storing the configuration for an {@link AlphaStm}. So instead
 * of having to deal with ever growing number of arguments, the AlphaStm constructor only
 * needs a single argument.
 * <p/>
 * Once the AlphaStm is constructed, changes made to the config object is not visible to
 * the stm that received. It wil have copied all fields, and doesn't read from the config
 * after creation anymore.
 * <p/>
 * AlphaStmConfig is not thread-safe. It can be shared between threads as long as no changes
 * are to the config and there is a save handover point from construction to usage. A volatile
 * variable or mutex would do the trick. As long as there is a happens before relation
 * between the write and the read expressed in terms of the JMM.
 * <p/>
 * A config will always be checked by the constructor of the stm if all fields are correctly
 * initialized.
 *
 * @author Peter Veentjer.
 */
public final class AlphaStmConfig {

    public static AlphaStmConfig createDebugConfig() {
        AlphaStmConfig config = new AlphaStmConfig();
        config.loggingPossible = true;
        return config;
    }

    public static AlphaStmConfig createFastConfig() {
        AlphaStmConfig config = new AlphaStmConfig();
        config.profiler = null;
        config.loggingPossible = false;
        config.clock = new RelaxedClock();
        return config;
    }

    public Clock clock = new StrictClock();

    public ProfileRepository profiler = new SimpleProfileRepository();

    public boolean loggingPossible = true;

    public CommitLockPolicy commitLockPolicy = GenericCommitLockPolicy.FAIL_FAST_BUT_RETRY;

    public RestartBackoffPolicy restartBackoffPolicy = ExponentialRestartBackoffPolicy.INSTANCE_10_MS_MAX;

    /**
     * Check if the AlphaStmConfig has been configured correctly.
     *
     * @throws IllegalStateException if the configuration isn't valid.
     */
    public void ensureValid() {
        if (commitLockPolicy == null) {
            throw new IllegalStateException("Invalid configuration, 'commitLockPolicy' can't be null");
        }

        if(clock == null){
            throw new IllegalStateException("Invalid configuration, 'clock' can't be null");
        }

        if(restartBackoffPolicy == null){
            throw new IllegalStateException("Invalid configuration, 'restartBackoffPolicy' can't be null");            
        }
    }
}
