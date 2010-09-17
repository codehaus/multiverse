package org.multiverse.stms.beta;

import org.multiverse.api.*;

/**
 * Contains the configuration for creating a BetaStm. Once the BetaStm is constructed, it doesn't keep any reference
 * to it, so changes will not reflect in the stm.
 */
public class BetaStmConfiguration {

    public StmCallback stmCallback;
    public boolean interruptible = false;
    public boolean readonly = false;
    public int spinCount = 16;
    public PessimisticLockLevel pessimisticLockLevel = PessimisticLockLevel.LockNone;
    public boolean dirtyCheck = true;
    public int minimalArrayTreeSize = 4;
    public boolean trackReads = true;
    public boolean blockingAllowed = true;
    public int maxRetries = 1000;
    public boolean speculativeConfigEnabled = true;
    public int maxArrayTransactionSize = 20;
    public BackoffPolicy backoffPolicy = ExponentialBackoffPolicy.MAX_100_MS;
    public long timeoutNs = Long.MAX_VALUE;
    public TraceLevel traceLevel = TraceLevel.None;
    public boolean writeSkewAllowed = true;
    public PropagationLevel propagationLevel = PropagationLevel.Requires;

    /**
     * Checks if the configuration is valid.
     *
     * @throws IllegalStateException if the configuration isn't valid.
     */
    public void validate(){
        if(writeSkewAllowed && !trackReads){
            throw new IllegalStateException("writeSkewAllowed can't be true if trackReads is false");
        }

        if(blockingAllowed && !trackReads){
            throw new IllegalStateException("blockingAllowed can't be true if trackReads is false");
        }

        if(spinCount<0){
            throw new IllegalStateException("spinCount can't be smaller than 0, but was "+spinCount);
        }

        if(pessimisticLockLevel == null){
            throw new IllegalStateException("pessimisticLockLevel can't be null");
        }

        if(minimalArrayTreeSize<0){
            throw new IllegalStateException("minimalArrayTreeSize can't be smaller than 0, but was "+minimalArrayTreeSize);
        }

        if(maxRetries<0){
            throw new IllegalStateException("maxRetries can't be smaller than 0, but was "+maxRetries);
        }

        if(maxArrayTransactionSize<2){
            throw new IllegalStateException("maxArrayTransactionSize can't be smaller than 2, but was "+maxArrayTransactionSize);
        }

        if(backoffPolicy == null){
            throw new IllegalStateException("backoffPolicy can't be null");
        }

        if(traceLevel == null){
            throw new IllegalStateException("traceLevel can't be null");
        }

        if(propagationLevel == null){
            throw new IllegalStateException("propagationLevel can't be null");
        }
    }
}
