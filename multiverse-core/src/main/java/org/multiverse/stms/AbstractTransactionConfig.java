package org.multiverse.stms;

import org.multiverse.api.TransactionConfig;
import org.multiverse.utils.clock.Clock;
import org.multiverse.utils.clock.StrictClock;
import org.multiverse.utils.restartbackoff.ExponentialRestartBackoffPolicy;
import org.multiverse.utils.restartbackoff.RestartBackoffPolicy;

/**
 * Contains the configuration for the AbstractTransaction.
 * <p/>
 * One advantage of this class is that it is a lot easier to add additional fields without having to change all
 * constructors. It also reduces the need for constructors with a lot of arguments.`
 *
 * @author Peter Veentjer.
 */
public class AbstractTransactionConfig implements TransactionConfig{

    public final Clock clock;
    public final RestartBackoffPolicy restartBackoffPolicy;
    public final String familyName;
    public final boolean readOnly;
    public final int maxRetryCount;
    public final boolean interruptible;
    public final boolean detectWriteSkew;
    public final boolean automaticReadTracking;

    /**
     * This method should be removed, only used for testing purposes.
     */
    public AbstractTransactionConfig() {
        this(new StrictClock(), ExponentialRestartBackoffPolicy.INSTANCE_10_MS_MAX, null, true, 1000, true, true,true);
    }

    public AbstractTransactionConfig(
            Clock clock, RestartBackoffPolicy restartBackoffPolicy, String familyName, boolean readOnly,
            int maxRetryCount, boolean interruptible, boolean detectWriteSkew, boolean automaticReadTracking) {
        assert clock != null;
        assert restartBackoffPolicy != null;
        
        this.clock = clock;
        this.familyName = familyName;
        this.readOnly = readOnly;
        this.restartBackoffPolicy = restartBackoffPolicy;
        this.maxRetryCount = maxRetryCount;
        this.interruptible = interruptible;
        this.automaticReadTracking = automaticReadTracking;
        this.detectWriteSkew = detectWriteSkew;

        if(readOnly && !automaticReadTracking && detectWriteSkew){
            throw new RuntimeException();
        }
    }

    @Override
    public String getFamilyName() {
        return familyName;
    }

    @Override
    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    @Override
    public boolean isInterruptible() {
        return interruptible;
    }

    @Override
    public boolean isReadonly() {
        return readOnly;
    }

    @Override
    public boolean detectWriteSkew() {
        return detectWriteSkew;
    }

    @Override
    public boolean automaticReadTracking() {
        return automaticReadTracking;
    }

    @Override
    public RestartBackoffPolicy getRestartBackoffPolicy() {
        return restartBackoffPolicy;
    }
}
