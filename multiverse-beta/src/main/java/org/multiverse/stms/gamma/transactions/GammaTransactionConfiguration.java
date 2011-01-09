package org.multiverse.stms.gamma.transactions;

import org.multiverse.api.*;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;

import java.util.List;

public class GammaTransactionConfiguration implements TransactionConfiguration, GammaConstants {

    public GammaStm stm;
    public int spinCount = 10;
    public int arraySize = 10;
    public boolean readonly = false;
    public int maxRetries = 1000;
    public boolean isInterruptible = true;
    public boolean isBlockingAllowed = true;
    public boolean dirtyCheck = true;
    public BackoffPolicy backoffPolicy = ExponentialBackoffPolicy.MAX_100_MS;
    public long timeoutNs = Long.MAX_VALUE;
    public int readLockMode = LOCKMODE_NONE;
    public int writeLockMode = LOCKMODE_NONE;
    private LockMode readLockLevel = LockMode.None;
    private LockMode writeLockLevel = LockMode.None;

    public GammaTransactionConfiguration(GammaStm stm) {
        this.stm = stm;
    }

    public GammaTransactionConfiguration(GammaStm stm, int arraySize) {
        this.stm = stm;
        this.arraySize = arraySize;
    }

    @Override
    public LockMode getReadLockLevel() {
        return readLockLevel;
    }

    @Override
    public LockMode getWriteLockLevel() {
        return writeLockLevel;
    }

    @Override
    public Stm getStm() {
        return stm;
    }

    @Override
    public IsolationLevel getIsolationLevel() {
        throw new TodoException();
    }

    @Override
    public long getTimeoutNs() {
        return timeoutNs;
    }

    @Override
    public PropagationLevel getPropagationLevel() {
        throw new TodoException();
    }

    @Override
    public TraceLevel getTraceLevel() {
        throw new TodoException();
    }

    @Override
    public BackoffPolicy getBackoffPolicy() {
        return backoffPolicy;
    }

    @Override
    public boolean isSpeculativeConfigEnabled() {
        throw new TodoException();
    }

    @Override
    public String getFamilyName() {
        throw new TodoException();
    }

    @Override
    public boolean isReadonly() {
        return readonly;
    }

    @Override
    public int getSpinCount() {
        return spinCount;
    }

    @Override
    public LockLevel getLockLevel() {
        throw new TodoException();
    }

    @Override
    public boolean isDirtyCheckEnabled() {
        return dirtyCheck;
    }

    @Override
    public boolean isReadTrackingEnabled() {
        throw new TodoException();
    }

    @Override
    public boolean isBlockingAllowed() {
        return isBlockingAllowed;
    }

    @Override
    public boolean isInterruptible() {
        return isInterruptible;
    }

    @Override
    public List<TransactionLifecycleListener> getPermanentListeners() {
        throw new TodoException();
    }

    @Override
    public int getMaxRetries() {
        return maxRetries;
    }
}
