package org.multiverse.stms.beta;

import org.multiverse.api.*;

import static java.lang.String.format;

/**
 * Contains the configuration for creating a BetaStm. Once the BetaStm is constructed, it doesn't keep any reference
 * to it, so changes will not reflect in the stm once constructed.
 *
 * @author Peter Veentjer.
 */
public class BetaStmConfiguration {

    public PropagationLevel propagationLevel = PropagationLevel.Requires;
    public IsolationLevel isolationLevel = IsolationLevel.Snapshot;
    public LockLevel level = LockLevel.LockNone;
    public boolean blockingAllowed = true;
    public boolean interruptible = false;
    public long timeoutNs = Long.MAX_VALUE;
    public boolean readonly = false;
    public int spinCount = 16;
    public boolean dirtyCheck = true;
    public int minimalArrayTreeSize = 4;
    public boolean trackReads = true;
    public int maxRetries = 1000;
    public boolean speculativeConfigEnabled = true;
    public int maxArrayTransactionSize = 20;
    public BackoffPolicy backoffPolicy = ExponentialBackoffPolicy.MAX_100_MS;
    public TraceLevel traceLevel = TraceLevel.None;

    /**
     * Checks if the configuration is valid.
     *
     * @throws IllegalStateException if the configuration isn't valid.
     */
    public void validate() {
        if (isolationLevel == null) {
            throw new IllegalStateException("isolationLevel can't be null");
        }

        if (isolationLevel.isWriteSkewAllowed() && !trackReads) {
            throw new IllegalStateException(format("isolation level '%s' can't be combined with readtracking is false" +
                    "since it is needed to prevent the writeskew problem", isolationLevel));
        }

        if (blockingAllowed && !trackReads) {
            throw new IllegalStateException("blockingAllowed can't be true if trackReads is false");
        }

        if (spinCount < 0) {
            throw new IllegalStateException("spinCount can't be smaller than 0, but was " + spinCount);
        }

        if (level == null) {
            throw new IllegalStateException("lockLevel can't be null");
        }

        if (minimalArrayTreeSize < 0) {
            throw new IllegalStateException("minimalArrayTreeSize can't be smaller than 0, but was "
                    + minimalArrayTreeSize);
        }

        if (maxRetries < 0) {
            throw new IllegalStateException("maxRetries can't be smaller than 0, but was " + maxRetries);
        }

        if (maxArrayTransactionSize < 2) {
            throw new IllegalStateException("maxArrayTransactionSize can't be smaller than 2, but was "
                    + maxArrayTransactionSize);
        }

        if (backoffPolicy == null) {
            throw new IllegalStateException("backoffPolicy can't be null");
        }

        if (traceLevel == null) {
            throw new IllegalStateException("traceLevel can't be null");
        }

        if (propagationLevel == null) {
            throw new IllegalStateException("propagationLevel can't be null");
        }
    }
}
