package org.multiverse.api;

import org.multiverse.stms.beta.BetaStm;

public interface TransactionConfiguration {

    long getTimeoutNs();

    PropagationLevel getPropagationLevel();

    TraceLevel getTraceLevel();

    BackoffPolicy getBackoffPolicy();

    boolean isSpeculativeConfigEnabled();

    String getFamilyName();

    boolean isReadonly();

    int getSpinCount();

    boolean isLockReads();

    boolean isLockWrites();

    PessimisticLockLevel getPessimisticLockLevel();

    boolean isDirtyCheck();

    boolean isWriteSkewAllowed();

    BetaStm getStm();

    int getMinimalArrayTreeSize();

    boolean isReadTrackingEnabled();

    boolean isBlockingAllowed();

    int getMaxRetries();
}
