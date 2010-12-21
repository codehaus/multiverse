package org.multiverse.stms.beta.transactions;

import org.multiverse.api.*;
import org.multiverse.api.exceptions.IllegalTransactionFactoryException;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConfiguration;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.conflictcounters.GlobalConflictCounter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.String.format;

/**
 * Configuration for the BetaTransaction. Should not be changed from the outside.
 *
 * @author Peter Veentjer
 */
public final class BetaTransactionConfiguration implements TransactionConfiguration, BetaStmConstants {

    public final static AtomicLong idGenerator = new AtomicLong();

    public final BetaStm stm;
    public final GlobalConflictCounter globalConflictCounter;
    public final AtomicReference<SpeculativeBetaConfiguration> speculativeConfiguration
            = new AtomicReference<SpeculativeBetaConfiguration>();
    public PropagationLevel propagationLevel;
    public IsolationLevel isolationLevel;
    public boolean writeSkewAllowed;
    public boolean inconsistentReadAllowed;
    public LockLevel lockLevel;
    public int readLockMode;
    public int writeLockMode;
    public final String familyName;
    public final boolean isAnonymous;
    public boolean interruptible = false;
    public boolean durable = false;
    public boolean readonly = false;
    public int spinCount = 16;
    public boolean dirtyCheck = true;
    public int minimalArrayTreeSize = 4;
    public boolean trackReads = true;
    public boolean blockingAllowed = true;
    public int maxRetries;
    public boolean speculativeConfigEnabled = true;
    public int maxArrayTransactionSize;
    public BackoffPolicy backoffPolicy;
    public long timeoutNs = Long.MAX_VALUE;
    public TraceLevel traceLevel = TraceLevel.None;

    public ArrayList<TransactionLifecycleListener> permanentListeners;

    public BetaTransactionConfiguration(BetaStm stm) {
        this(stm, new BetaStmConfiguration());
    }

    public BetaTransactionConfiguration(BetaStm stm, String familyName, boolean isAnonymous) {
        if (stm == null) {
            throw new NullPointerException();
        }

        this.stm = stm;
        this.familyName = familyName;
        this.isAnonymous = isAnonymous;
        this.globalConflictCounter = stm.getGlobalConflictCounter();
    }

    public BetaTransactionConfiguration(BetaStm stm, BetaStmConfiguration configuration) {
        if (stm == null) {
            throw new NullPointerException();
        }

        this.stm = stm;
        this.interruptible = configuration.interruptible;
        this.readonly = configuration.readonly;
        this.spinCount = configuration.spinCount;
        this.lockLevel = configuration.level;
        this.dirtyCheck = configuration.dirtyCheck;
        this.minimalArrayTreeSize = configuration.minimalArrayTreeSize;
        this.trackReads = configuration.trackReads;
        this.blockingAllowed = configuration.blockingAllowed;
        this.maxRetries = configuration.maxRetries;
        this.speculativeConfigEnabled = configuration.speculativeConfigEnabled;
        this.maxArrayTransactionSize = configuration.maxArrayTransactionSize;
        this.backoffPolicy = configuration.backoffPolicy;
        this.timeoutNs = configuration.timeoutNs;
        this.traceLevel = configuration.traceLevel;
        this.isolationLevel = configuration.isolationLevel;
        this.writeSkewAllowed = isolationLevel.isWriteSkewAllowed();
        this.inconsistentReadAllowed = isolationLevel.isInconsistentReadAllowed();
        this.propagationLevel = configuration.propagationLevel;
        this.familyName = "anonymoustransaction-" + idGenerator.incrementAndGet();
        this.isAnonymous = true;
        this.globalConflictCounter = stm.getGlobalConflictCounter();
    }

    public BetaTransactionConfiguration(BetaStm stm, int maxArrayTransactionSize) {
        this(stm);
        this.maxArrayTransactionSize = maxArrayTransactionSize;
    }

    @Override
    public IsolationLevel getIsolationLevel() {
        return isolationLevel;
    }

    public boolean hasTimeout() {
        return timeoutNs != Long.MAX_VALUE;
    }

    public SpeculativeBetaConfiguration getSpeculativeConfiguration() {
        return speculativeConfiguration.get();
    }

    @Override
    public long getTimeoutNs() {
        return timeoutNs;
    }

    @Override
    public TraceLevel getTraceLevel() {
        return traceLevel;
    }

    @Override
    public boolean isInterruptible() {
        return interruptible;
    }

    @Override
    public BackoffPolicy getBackoffPolicy() {
        return backoffPolicy;
    }

    public int getMaxArrayTransactionSize() {
        return maxArrayTransactionSize;
    }

    @Override
    public boolean isSpeculativeConfigEnabled() {
        return speculativeConfigEnabled;
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
    public int getSpinCount() {
        return spinCount;
    }

    @Override
    public LockLevel getLockLevel() {
        return lockLevel;
    }

    @Override
    public boolean isDirtyCheckEnabled() {
        return dirtyCheck;
    }

    @Override
    public BetaStm getStm() {
        return stm;
    }

    public GlobalConflictCounter getGlobalConflictCounter() {
        return globalConflictCounter;
    }

    @Override
    public boolean isReadTrackingEnabled() {
        return trackReads;
    }

    @Override
    public boolean isBlockingAllowed() {
        return blockingAllowed;
    }

    @Override
    public int getMaxRetries() {
        return maxRetries;
    }

    @Override
    public PropagationLevel getPropagationLevel() {
        return propagationLevel;
    }

    @Override
    public List<TransactionLifecycleListener> getPermanentListeners() {
        if (permanentListeners == null) {
            return Collections.EMPTY_LIST;
        }
        return Collections.unmodifiableList(permanentListeners);
    }

    public void needsOrelse() {
        while (true) {
            SpeculativeBetaConfiguration current = speculativeConfiguration.get();
            SpeculativeBetaConfiguration update = current.createWithOrElseRequired();
            if (speculativeConfiguration.compareAndSet(current, update)) {
                return;
            }
        }
    }

    public void needsListeners() {
        while (true) {
            SpeculativeBetaConfiguration current = speculativeConfiguration.get();
            SpeculativeBetaConfiguration update = current.createWithListenersRequired();
            if (speculativeConfiguration.compareAndSet(current, update)) {
                return;
            }
        }
    }

    public void needsCommute() {
        while (true) {
            SpeculativeBetaConfiguration current = speculativeConfiguration.get();
            SpeculativeBetaConfiguration update = current.createWithCommuteRequired();
            if (speculativeConfiguration.compareAndSet(current, update)) {
                return;
            }
        }
    }

    public void needsMinimalTransactionLength(int newLength) {
        while (true) {
            SpeculativeBetaConfiguration current = speculativeConfiguration.get();
            SpeculativeBetaConfiguration next = current.createWithMinimalLength(newLength);

            if (speculativeConfiguration.compareAndSet(current, next)) {
                return;
            }
        }
    }

    public BetaTransactionConfiguration init() {
        if (!writeSkewAllowed && !trackReads && !readonly) {
            String msg = format("'[%s] If no writeskew is allowed, read tracking should be enabled", familyName);
            throw new IllegalTransactionFactoryException(msg);
        }

        if (blockingAllowed && !trackReads) {
            String msg = format("[%s] If blocking is allowed, read tracking should be enabled", familyName);
            throw new IllegalTransactionFactoryException(msg);
        }

        if (lockLevel == LockLevel.CommitLockReads && !trackReads) {
            String msg = format("[%s] If all reads should be locked, read tracking should be enabled", familyName);
            throw new IllegalTransactionFactoryException(msg);
        }

        if (speculativeConfigEnabled) {
            boolean isFat = !writeSkewAllowed;

            if (speculativeConfiguration.get() == null) {
                SpeculativeBetaConfiguration newSpeculativeConfiguration = new SpeculativeBetaConfiguration(isFat);
                speculativeConfiguration.compareAndSet(null, newSpeculativeConfiguration);
            }
        }

        return this;
    }

    public BetaTransactionConfiguration setTimeoutNs(long timeoutNs) {
        if (timeoutNs < 0) {
            throw new IllegalArgumentException("timeoutNs can't be smaller than 0");
        }

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.lockLevel = lockLevel;
        config.readLockMode = lockLevel.getReadLockMode();
        config.writeLockMode = lockLevel.getWriteLockMode();
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxArrayTransactionSize = maxArrayTransactionSize;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.isolationLevel = isolationLevel;
        config.writeSkewAllowed = isolationLevel.isWriteSkewAllowed();
        config.inconsistentReadAllowed = isolationLevel.isInconsistentReadAllowed();
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        return config;
    }

    public BetaTransactionConfiguration setFamilyName(String familyName) {
        if (familyName == null) {
            throw new NullPointerException("familyName can't be null");
        }

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, familyName, false);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.lockLevel = lockLevel;
        config.readLockMode = lockLevel.getReadLockMode();
        config.writeLockMode = lockLevel.getWriteLockMode();
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxArrayTransactionSize = maxArrayTransactionSize;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.writeSkewAllowed = isolationLevel.isWriteSkewAllowed();
        config.inconsistentReadAllowed = isolationLevel.isInconsistentReadAllowed();
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        return config;
    }

    public BetaTransactionConfiguration setMaxRetries(int maxRetries) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries can't be smaller than 0");
        }

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.lockLevel = lockLevel;
        config.readLockMode = lockLevel.getReadLockMode();
        config.writeLockMode = lockLevel.getWriteLockMode();
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxArrayTransactionSize = maxArrayTransactionSize;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.isolationLevel = isolationLevel;
        config.writeSkewAllowed = isolationLevel.isWriteSkewAllowed();
        config.inconsistentReadAllowed = isolationLevel.isInconsistentReadAllowed();
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        return config;
    }

    public BetaTransactionConfiguration setReadTrackingEnabled(boolean trackReads) {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.lockLevel = lockLevel;
        config.readLockMode = lockLevel.getReadLockMode();
        config.writeLockMode = lockLevel.getWriteLockMode();
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxArrayTransactionSize = maxArrayTransactionSize;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.isolationLevel = isolationLevel;
        config.writeSkewAllowed = isolationLevel.isWriteSkewAllowed();
        config.inconsistentReadAllowed = isolationLevel.isInconsistentReadAllowed();
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        return config;
    }

    public BetaTransactionConfiguration setSpeculativeConfigurationEnabled(boolean speculativeConfigEnabled) {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.lockLevel = lockLevel;
        config.readLockMode = lockLevel.getReadLockMode();
        config.writeLockMode = lockLevel.getWriteLockMode();
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxArrayTransactionSize = maxArrayTransactionSize;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.isolationLevel = isolationLevel;
        config.writeSkewAllowed = isolationLevel.isWriteSkewAllowed();
        config.inconsistentReadAllowed = isolationLevel.isInconsistentReadAllowed();
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        return config;
    }

    public BetaTransactionConfiguration setReadonly(boolean readonly) {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.lockLevel = lockLevel;
        config.readLockMode = lockLevel.getReadLockMode();
        config.writeLockMode = lockLevel.getWriteLockMode();
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxArrayTransactionSize = maxArrayTransactionSize;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.isolationLevel = isolationLevel;
        config.writeSkewAllowed = isolationLevel.isWriteSkewAllowed();
        config.inconsistentReadAllowed = isolationLevel.isInconsistentReadAllowed();
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        return config;
    }

    public BetaTransactionConfiguration setDirtyCheckEnabled(boolean dirtyCheck) {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.lockLevel = lockLevel;
        config.readLockMode = lockLevel.getReadLockMode();
        config.writeLockMode = lockLevel.getWriteLockMode();
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxArrayTransactionSize = maxArrayTransactionSize;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.isolationLevel = isolationLevel;
        config.writeSkewAllowed = isolationLevel.isWriteSkewAllowed();
        config.inconsistentReadAllowed = isolationLevel.isInconsistentReadAllowed();
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        return config;
    }

    public BetaTransactionConfiguration setBlockingAllowed(boolean blockingEnabled) {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.lockLevel = lockLevel;
        config.readLockMode = lockLevel.getReadLockMode();
        config.writeLockMode = lockLevel.getWriteLockMode();
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingEnabled;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxArrayTransactionSize = maxArrayTransactionSize;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.isolationLevel = isolationLevel;
        config.writeSkewAllowed = isolationLevel.isWriteSkewAllowed();
        config.inconsistentReadAllowed = isolationLevel.isInconsistentReadAllowed();
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        return config;
    }

    public BetaTransactionConfiguration setInterruptible(boolean interruptible) {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.lockLevel = lockLevel;
        config.readLockMode = lockLevel.getReadLockMode();
        config.writeLockMode = lockLevel.getWriteLockMode();
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxArrayTransactionSize = maxArrayTransactionSize;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.isolationLevel = isolationLevel;
        config.writeSkewAllowed = isolationLevel.isWriteSkewAllowed();
        config.inconsistentReadAllowed = isolationLevel.isInconsistentReadAllowed();
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        return config;
    }

    public BetaTransactionConfiguration setSpinCount(int spinCount) {
        if (spinCount < 0) {
            throw new IllegalArgumentException("spinCount can't be smaller than 0");
        }

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.lockLevel = lockLevel;
        config.readLockMode = lockLevel.getReadLockMode();
        config.writeLockMode = lockLevel.getWriteLockMode();
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxArrayTransactionSize = maxArrayTransactionSize;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.isolationLevel = isolationLevel;
        config.writeSkewAllowed = isolationLevel.isWriteSkewAllowed();
        config.inconsistentReadAllowed = isolationLevel.isInconsistentReadAllowed();
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        return config;
    }

    public BetaTransactionConfiguration setLockLevel(LockLevel lockLevel) {
        if (lockLevel == null) {
            throw new NullPointerException("lockLevel can't be null");
        }

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.lockLevel = lockLevel;
        config.readLockMode = lockLevel.getReadLockMode();
        config.writeLockMode = lockLevel.getWriteLockMode();
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.durable = durable;
        config.maxArrayTransactionSize = maxArrayTransactionSize;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.isolationLevel = isolationLevel;
        config.writeSkewAllowed = isolationLevel.isWriteSkewAllowed();
        config.inconsistentReadAllowed = isolationLevel.isInconsistentReadAllowed();
        config.propagationLevel = propagationLevel;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.permanentListeners = permanentListeners;
        return config;
    }

    public BetaTransactionConfiguration setBackoffPolicy(BackoffPolicy backoffPolicy) {
        if (backoffPolicy == null) {
            throw new NullPointerException("backoffPolicy can't be null");
        }

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.lockLevel = lockLevel;
        config.readLockMode = lockLevel.getReadLockMode();
        config.writeLockMode = lockLevel.getWriteLockMode();
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxArrayTransactionSize = maxArrayTransactionSize;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.isolationLevel = isolationLevel;
        config.writeSkewAllowed = isolationLevel.isWriteSkewAllowed();
        config.inconsistentReadAllowed = isolationLevel.isInconsistentReadAllowed();
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        return config;
    }

    public BetaTransactionConfiguration setTraceLevel(TraceLevel traceLevel) {
        if (traceLevel == null) {
            throw new NullPointerException("traceLevel can't be null");
        }

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.lockLevel = lockLevel;
        config.readLockMode = lockLevel.getReadLockMode();
        config.writeLockMode = lockLevel.getWriteLockMode();
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxArrayTransactionSize = maxArrayTransactionSize;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.isolationLevel = isolationLevel;
        config.writeSkewAllowed = isolationLevel.isWriteSkewAllowed();
        config.inconsistentReadAllowed = isolationLevel.isInconsistentReadAllowed();
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        return config;
    }

    public BetaTransactionConfiguration setPropagationLevel(PropagationLevel propagationLevel) {
        if (propagationLevel == null) {
            throw new NullPointerException();
        }

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.lockLevel = lockLevel;
        config.readLockMode = lockLevel.getReadLockMode();
        config.writeLockMode = lockLevel.getWriteLockMode();
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxArrayTransactionSize = maxArrayTransactionSize;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.isolationLevel = isolationLevel;
        config.writeSkewAllowed = isolationLevel.isWriteSkewAllowed();
        config.inconsistentReadAllowed = isolationLevel.isInconsistentReadAllowed();
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        return config;
    }


    public BetaTransactionConfiguration setIsolationLevel(IsolationLevel isolationLevel) {
        if (isolationLevel == null) {
            throw new NullPointerException();
        }

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.lockLevel = lockLevel;
        config.readLockMode = lockLevel.getReadLockMode();
        config.writeLockMode = lockLevel.getWriteLockMode();
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxArrayTransactionSize = maxArrayTransactionSize;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        config.isolationLevel = isolationLevel;
        config.writeSkewAllowed = isolationLevel.isWriteSkewAllowed();
        config.inconsistentReadAllowed = isolationLevel.isInconsistentReadAllowed();
        return config;
    }

    public BetaTransactionConfiguration addPermanentListener(TransactionLifecycleListener listener) {
        if (listener == null) {
            throw new NullPointerException();
        }

        ArrayList<TransactionLifecycleListener> newPermanentListeners
                = new ArrayList<TransactionLifecycleListener>();
        if (permanentListeners != null) {
            newPermanentListeners.addAll(permanentListeners);
        }
        newPermanentListeners.add(listener);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.lockLevel = lockLevel;
        config.readLockMode = lockLevel.getReadLockMode();
        config.writeLockMode = lockLevel.getWriteLockMode();
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxArrayTransactionSize = maxArrayTransactionSize;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.isolationLevel = isolationLevel;
        config.writeSkewAllowed = isolationLevel.isWriteSkewAllowed();
        config.inconsistentReadAllowed = isolationLevel.isInconsistentReadAllowed();
        config.propagationLevel = propagationLevel;
        config.permanentListeners = newPermanentListeners;
        return config;
    }

    @Override
    public String toString() {
        return "BetaTransactionConfiguration{" +
                "familyName='" + familyName +
                ", propagationLevel=" + propagationLevel +
                ", isolationLevel=" + isolationLevel +
                ", lockLevel=" + lockLevel +
                ", traceLevel=" + traceLevel +
                ", readonly=" + readonly +
                ", speculativeConfiguration=" + speculativeConfiguration +
                ", spinCount=" + spinCount +
                ", dirtyCheck=" + dirtyCheck +
                ", minimalArrayTreeSize=" + minimalArrayTreeSize +
                ", trackReads=" + trackReads +
                ", maxRetries=" + maxRetries +
                ", speculativeConfigEnabled=" + speculativeConfigEnabled +
                ", maxArrayTransactionSize=" + maxArrayTransactionSize +
                ", isAnonymous=" + isAnonymous +
                ", backoffPolicy=" + backoffPolicy +
                ", blockingAllowed=" + blockingAllowed +
                ", timeoutNs=" + timeoutNs +
                ", interruptible=" + interruptible +
                ", permanentListeners=" + permanentListeners +
                '}';
    }
}
