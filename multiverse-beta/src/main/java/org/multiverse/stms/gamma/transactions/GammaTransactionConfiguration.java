package org.multiverse.stms.gamma.transactions;

import org.multiverse.api.*;
import org.multiverse.api.exceptions.IllegalTransactionFactoryException;
import org.multiverse.api.lifecycle.TransactionListener;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.GammaStmConfiguration;
import org.multiverse.stms.gamma.GlobalConflictCounter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.String.format;

@SuppressWarnings({"OverlyComplexClass", "ClassWithTooManyFields"})
public final class GammaTransactionConfiguration implements TransactionConfiguration, GammaConstants {

    public final static AtomicLong idGenerator = new AtomicLong();

    public final GammaStm stm;
    public final GlobalConflictCounter globalConflictCounter;
    public final AtomicReference<SpeculativeGammaConfiguration> speculativeConfiguration
            = new AtomicReference<SpeculativeGammaConfiguration>();
    public PropagationLevel propagationLevel;
    public IsolationLevel isolationLevel;
    public boolean writeSkewAllowed;
    public boolean inconsistentReadAllowed;
    public LockMode readLockMode;
    public LockMode writeLockMode;
    public int readLockModeAsInt;
    public int writeLockModeAsInt;
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
    public int maxFixedLengthTransactionSize;
    public BackoffPolicy backoffPolicy;
    public long timeoutNs = Long.MAX_VALUE;
    public TraceLevel traceLevel = TraceLevel.None;
    public boolean controlFlowErrorsReused = false;

    public ArrayList<TransactionListener> permanentListeners;

    public GammaTransactionConfiguration(GammaStm stm) {
        this(stm, new GammaStmConfiguration());
    }

    public GammaTransactionConfiguration(GammaStm stm, String familyName, boolean isAnonymous) {
        if (stm == null) {
            throw new NullPointerException();
        }

        this.stm = stm;
        this.familyName = familyName;
        this.isAnonymous = isAnonymous;
        this.globalConflictCounter = stm.globalConflictCounter;
    }

    public GammaTransactionConfiguration(GammaStm stm, GammaStmConfiguration configuration) {
        if (stm == null) {
            throw new NullPointerException();
        }

        this.stm = stm;
        this.globalConflictCounter = stm.getGlobalConflictCounter();
        this.interruptible = configuration.interruptible;
        this.readonly = configuration.readonly;
        this.spinCount = configuration.spinCount;
        this.readLockMode = configuration.readLockMode;
        this.writeLockMode = configuration.writeLockMode;
        this.dirtyCheck = configuration.dirtyCheck;
        this.minimalArrayTreeSize = configuration.minimalArrayTreeSize;
        this.trackReads = configuration.trackReads;
        this.blockingAllowed = configuration.blockingAllowed;
        this.maxRetries = configuration.maxRetries;
        this.speculativeConfigEnabled = configuration.speculativeConfigEnabled;
        this.maxFixedLengthTransactionSize = configuration.maxArrayTransactionSize;
        this.backoffPolicy = configuration.backoffPolicy;
        this.timeoutNs = configuration.timeoutNs;
        this.traceLevel = configuration.traceLevel;
        this.isolationLevel = configuration.isolationLevel;
        this.writeSkewAllowed = isolationLevel.isWriteSkewAllowed();
        this.inconsistentReadAllowed = isolationLevel.isInconsistentReadAllowed();
        this.propagationLevel = configuration.propagationLevel;
        this.controlFlowErrorsReused = configuration.controlFlowErrorsReused;
        this.familyName = "anonymoustransaction-" + idGenerator.incrementAndGet();
        this.isAnonymous = true;
    }

    public GammaTransactionConfiguration(GammaStm stm, int maxFixedLengthTransactionSize) {
        this(stm);
        this.maxFixedLengthTransactionSize = maxFixedLengthTransactionSize;
    }

    @Override
    public LockMode getReadLockMode() {
        return readLockMode;
    }

    @Override
    public LockMode getWriteLockMode() {
        return writeLockMode;
    }

    @Override
    public IsolationLevel getIsolationLevel() {
        return isolationLevel;
    }

    public boolean hasTimeout() {
        return timeoutNs != Long.MAX_VALUE;
    }

    @Override
    public boolean isControlFlowErrorsReused() {
        return controlFlowErrorsReused;
    }

    public SpeculativeGammaConfiguration getSpeculativeConfiguration() {
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

    public int getMaxFixedLengthTransactionSize() {
        return maxFixedLengthTransactionSize;
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
    public boolean isDirtyCheckEnabled() {
        return dirtyCheck;
    }

    @Override
    public GammaStm getStm() {
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
    public List<TransactionListener> getPermanentListeners() {
        if (permanentListeners == null) {
            return Collections.EMPTY_LIST;
        }
        return Collections.unmodifiableList(permanentListeners);
    }

    public void setSpeculativeConfigurationToUseNonRefType() {
        while (true) {
            SpeculativeGammaConfiguration current = speculativeConfiguration.get();
            SpeculativeGammaConfiguration update = current.createWithNonRefType();
            if (speculativeConfiguration.compareAndSet(current, update)) {
                return;
            }
        }
    }

    public void setSpeculativeConfigurationTouseOrElse() {
        while (true) {
            SpeculativeGammaConfiguration current = speculativeConfiguration.get();
            SpeculativeGammaConfiguration update = current.createWithOrElseRequired();
            if (speculativeConfiguration.compareAndSet(current, update)) {
                return;
            }
        }
    }

    public void setSpeculativeConfigurationToUseListeners() {
        while (true) {
            SpeculativeGammaConfiguration current = speculativeConfiguration.get();
            SpeculativeGammaConfiguration update = current.createWithListenersRequired();
            if (speculativeConfiguration.compareAndSet(current, update)) {
                return;
            }
        }
    }

    public void setSpeculativeConfigurationToUseCommute() {
        while (true) {
            SpeculativeGammaConfiguration current = speculativeConfiguration.get();
            SpeculativeGammaConfiguration update = current.createWithCommuteRequired();
            if (speculativeConfiguration.compareAndSet(current, update)) {
                return;
            }
        }
    }

    public void setSpeculativeConfigurationToUseExplicitLocking() {
        while (true) {
            SpeculativeGammaConfiguration current = speculativeConfiguration.get();
            SpeculativeGammaConfiguration update = current.createWithLocksRequired();
            if (speculativeConfiguration.compareAndSet(current, update)) {
                return;
            }
        }
    }

    public void setSpeculativeConfigurationToUseConstructedObjects() {
        while (true) {
            SpeculativeGammaConfiguration current = speculativeConfiguration.get();
            SpeculativeGammaConfiguration next = current.createWithConstructedObjectsRequired();

            if (speculativeConfiguration.compareAndSet(current, next)) {
                return;
            }
        }
    }

    public void needsMinimalTransactionLength(int newLength) {
        while (true) {
            SpeculativeGammaConfiguration current = speculativeConfiguration.get();
            SpeculativeGammaConfiguration next = current.createWithMinimalLength(newLength);

            if (speculativeConfiguration.compareAndSet(current, next)) {
                return;
            }
        }
    }

    public GammaTransactionConfiguration init() {
        if (!writeSkewAllowed && !trackReads && !readonly) {
            String msg = format("'[%s] If no writeskew is allowed, read tracking should be enabled", familyName);
            throw new IllegalTransactionFactoryException(msg);
        }

        if (blockingAllowed && !trackReads) {
            String msg = format("[%s] If blocking is allowed, read tracking should be enabled", familyName);
            throw new IllegalTransactionFactoryException(msg);
        }

        if (readLockMode != LockMode.None && !trackReads) {
            String msg = format("[%s] If readLockMode is [%s] , read tracking should be enabled", familyName, readLockMode);
            throw new IllegalTransactionFactoryException(msg);
        }

        if (speculativeConfigEnabled) {
            boolean isFat = !writeSkewAllowed;

            if (speculativeConfiguration.get() == null) {
                SpeculativeGammaConfiguration newSpeculativeConfiguration = new SpeculativeGammaConfiguration(isFat);
                speculativeConfiguration.compareAndSet(null, newSpeculativeConfiguration);
            }
        }

        return this;
    }

    public GammaTransactionConfiguration setTimeoutNs(long timeoutNs) {
        if (timeoutNs < 0) {
            throw new IllegalArgumentException("timeoutNs can't be smaller than 0");
        }

        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.readLockMode = readLockMode;
        config.readLockModeAsInt = readLockModeAsInt;
        config.writeLockMode = writeLockMode;
        config.writeLockModeAsInt = writeLockModeAsInt;
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxFixedLengthTransactionSize = maxFixedLengthTransactionSize;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.isolationLevel = isolationLevel;
        config.writeSkewAllowed = isolationLevel.isWriteSkewAllowed();
        config.inconsistentReadAllowed = isolationLevel.isInconsistentReadAllowed();
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        config.controlFlowErrorsReused = controlFlowErrorsReused;
        return config;
    }

    public GammaTransactionConfiguration setFamilyName(String familyName) {
        if (familyName == null) {
            throw new NullPointerException("familyName can't be null");
        }

        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm, familyName, false);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.readLockMode = readLockMode;
        config.readLockModeAsInt = readLockModeAsInt;
        config.writeLockMode = writeLockMode;
        config.writeLockModeAsInt = writeLockModeAsInt;
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxFixedLengthTransactionSize = maxFixedLengthTransactionSize;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.writeSkewAllowed = isolationLevel.isWriteSkewAllowed();
        config.inconsistentReadAllowed = isolationLevel.isInconsistentReadAllowed();
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        config.controlFlowErrorsReused = controlFlowErrorsReused;
        return config;
    }

    public GammaTransactionConfiguration setMaxRetries(int maxRetries) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries can't be smaller than 0");
        }

        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.readLockMode = readLockMode;
        config.readLockModeAsInt = readLockModeAsInt;
        config.writeLockMode = writeLockMode;
        config.writeLockModeAsInt = writeLockModeAsInt;
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxFixedLengthTransactionSize = maxFixedLengthTransactionSize;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.isolationLevel = isolationLevel;
        config.writeSkewAllowed = isolationLevel.isWriteSkewAllowed();
        config.inconsistentReadAllowed = isolationLevel.isInconsistentReadAllowed();
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        config.controlFlowErrorsReused = controlFlowErrorsReused;
        return config;
    }

    public GammaTransactionConfiguration setReadTrackingEnabled(boolean trackReads) {
        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.readLockMode = readLockMode;
        config.readLockModeAsInt = readLockModeAsInt;
        config.writeLockMode = writeLockMode;
        config.writeLockModeAsInt = writeLockModeAsInt;
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxFixedLengthTransactionSize = maxFixedLengthTransactionSize;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.isolationLevel = isolationLevel;
        config.writeSkewAllowed = isolationLevel.isWriteSkewAllowed();
        config.inconsistentReadAllowed = isolationLevel.isInconsistentReadAllowed();
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        config.controlFlowErrorsReused = controlFlowErrorsReused;
        return config;
    }

    public GammaTransactionConfiguration setSpeculativeConfigurationEnabled(boolean speculativeConfigEnabled) {
        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.readLockMode = readLockMode;
        config.readLockModeAsInt = readLockModeAsInt;
        config.writeLockMode = writeLockMode;
        config.writeLockModeAsInt = writeLockModeAsInt;
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxFixedLengthTransactionSize = maxFixedLengthTransactionSize;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.isolationLevel = isolationLevel;
        config.writeSkewAllowed = isolationLevel.isWriteSkewAllowed();
        config.inconsistentReadAllowed = isolationLevel.isInconsistentReadAllowed();
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        config.controlFlowErrorsReused = controlFlowErrorsReused;
        return config;
    }

    public GammaTransactionConfiguration setReadonly(boolean readonly) {
        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.readLockMode = readLockMode;
        config.readLockModeAsInt = readLockModeAsInt;
        config.writeLockMode = writeLockMode;
        config.writeLockModeAsInt = writeLockModeAsInt;
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxFixedLengthTransactionSize = maxFixedLengthTransactionSize;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.isolationLevel = isolationLevel;
        config.writeSkewAllowed = isolationLevel.isWriteSkewAllowed();
        config.inconsistentReadAllowed = isolationLevel.isInconsistentReadAllowed();
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        config.controlFlowErrorsReused = controlFlowErrorsReused;
        return config;
    }

    public GammaTransactionConfiguration setDirtyCheckEnabled(boolean dirtyCheck) {
        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.readLockMode = readLockMode;
        config.readLockModeAsInt = readLockModeAsInt;
        config.writeLockMode = writeLockMode;
        config.writeLockModeAsInt = writeLockModeAsInt;
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxFixedLengthTransactionSize = maxFixedLengthTransactionSize;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.isolationLevel = isolationLevel;
        config.writeSkewAllowed = isolationLevel.isWriteSkewAllowed();
        config.inconsistentReadAllowed = isolationLevel.isInconsistentReadAllowed();
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        config.controlFlowErrorsReused = controlFlowErrorsReused;
        return config;
    }

    public GammaTransactionConfiguration setBlockingAllowed(boolean blockingEnabled) {
        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.readLockMode = readLockMode;
        config.readLockModeAsInt = readLockModeAsInt;
        config.writeLockMode = writeLockMode;
        config.writeLockModeAsInt = writeLockModeAsInt;
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingEnabled;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxFixedLengthTransactionSize = maxFixedLengthTransactionSize;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.isolationLevel = isolationLevel;
        config.writeSkewAllowed = isolationLevel.isWriteSkewAllowed();
        config.inconsistentReadAllowed = isolationLevel.isInconsistentReadAllowed();
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        config.controlFlowErrorsReused = controlFlowErrorsReused;
        return config;
    }

    public GammaTransactionConfiguration setInterruptible(boolean interruptible) {
        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.readLockMode = readLockMode;
        config.readLockModeAsInt = readLockModeAsInt;
        config.writeLockMode = writeLockMode;
        config.writeLockModeAsInt = writeLockModeAsInt;
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxFixedLengthTransactionSize = maxFixedLengthTransactionSize;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.isolationLevel = isolationLevel;
        config.writeSkewAllowed = isolationLevel.isWriteSkewAllowed();
        config.inconsistentReadAllowed = isolationLevel.isInconsistentReadAllowed();
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        config.controlFlowErrorsReused = controlFlowErrorsReused;
        return config;
    }

    public GammaTransactionConfiguration setControlFlowErrorsReused(boolean controlFlowErrorsReused) {
        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.readLockMode = readLockMode;
        config.readLockModeAsInt = readLockModeAsInt;
        config.writeLockMode = writeLockMode;
        config.writeLockModeAsInt = writeLockModeAsInt;
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxFixedLengthTransactionSize = maxFixedLengthTransactionSize;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.isolationLevel = isolationLevel;
        config.writeSkewAllowed = isolationLevel.isWriteSkewAllowed();
        config.inconsistentReadAllowed = isolationLevel.isInconsistentReadAllowed();
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        config.controlFlowErrorsReused = controlFlowErrorsReused;
        return config;
    }

    public GammaTransactionConfiguration setSpinCount(int spinCount) {
        if (spinCount < 0) {
            throw new IllegalArgumentException("spinCount can't be smaller than 0");
        }

        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.readLockMode = readLockMode;
        config.readLockModeAsInt = readLockModeAsInt;
        config.writeLockMode = writeLockMode;
        config.writeLockModeAsInt = writeLockModeAsInt;
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxFixedLengthTransactionSize = maxFixedLengthTransactionSize;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.isolationLevel = isolationLevel;
        config.writeSkewAllowed = isolationLevel.isWriteSkewAllowed();
        config.inconsistentReadAllowed = isolationLevel.isInconsistentReadAllowed();
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        config.controlFlowErrorsReused = controlFlowErrorsReused;
        return config;
    }

    public GammaTransactionConfiguration setBackoffPolicy(BackoffPolicy backoffPolicy) {
        if (backoffPolicy == null) {
            throw new NullPointerException("backoffPolicy can't be null");
        }

        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.readLockMode = readLockMode;
        config.readLockModeAsInt = readLockModeAsInt;
        config.writeLockMode = writeLockMode;
        config.writeLockModeAsInt = writeLockModeAsInt;
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxFixedLengthTransactionSize = maxFixedLengthTransactionSize;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.isolationLevel = isolationLevel;
        config.writeSkewAllowed = isolationLevel.isWriteSkewAllowed();
        config.inconsistentReadAllowed = isolationLevel.isInconsistentReadAllowed();
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        config.controlFlowErrorsReused = controlFlowErrorsReused;
        return config;
    }

    public GammaTransactionConfiguration setTraceLevel(TraceLevel traceLevel) {
        if (traceLevel == null) {
            throw new NullPointerException("traceLevel can't be null");
        }

        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.readLockMode = readLockMode;
        config.readLockModeAsInt = readLockModeAsInt;
        config.writeLockMode = writeLockMode;
        config.writeLockModeAsInt = writeLockModeAsInt;
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxFixedLengthTransactionSize = maxFixedLengthTransactionSize;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.isolationLevel = isolationLevel;
        config.writeSkewAllowed = isolationLevel.isWriteSkewAllowed();
        config.inconsistentReadAllowed = isolationLevel.isInconsistentReadAllowed();
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        config.controlFlowErrorsReused = controlFlowErrorsReused;
        return config;
    }

    public GammaTransactionConfiguration setPropagationLevel(PropagationLevel propagationLevel) {
        if (propagationLevel == null) {
            throw new NullPointerException();
        }

        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.readLockMode = readLockMode;
        config.readLockModeAsInt = readLockModeAsInt;
        config.writeLockMode = writeLockMode;
        config.writeLockModeAsInt = writeLockModeAsInt;
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxFixedLengthTransactionSize = maxFixedLengthTransactionSize;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.isolationLevel = isolationLevel;
        config.writeSkewAllowed = isolationLevel.isWriteSkewAllowed();
        config.inconsistentReadAllowed = isolationLevel.isInconsistentReadAllowed();
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        config.controlFlowErrorsReused = controlFlowErrorsReused;
        return config;
    }


    public GammaTransactionConfiguration setIsolationLevel(IsolationLevel isolationLevel) {
        if (isolationLevel == null) {
            throw new NullPointerException();
        }

        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.readLockMode = readLockMode;
        config.readLockModeAsInt = readLockModeAsInt;
        config.writeLockMode = writeLockMode;
        config.writeLockModeAsInt = writeLockModeAsInt;
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxFixedLengthTransactionSize = maxFixedLengthTransactionSize;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        config.isolationLevel = isolationLevel;
        config.writeSkewAllowed = isolationLevel.isWriteSkewAllowed();
        config.inconsistentReadAllowed = isolationLevel.isInconsistentReadAllowed();
        config.controlFlowErrorsReused = controlFlowErrorsReused;
        return config;
    }

    public GammaTransactionConfiguration setWriteLockMode(LockMode writeLockMode) {
        if (writeLockMode == null) {
            throw new NullPointerException();
        }

        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.readLockMode = readLockMode;
        config.readLockModeAsInt = readLockModeAsInt;
        config.writeLockMode = writeLockMode;
        config.writeLockModeAsInt = writeLockMode.asInt();
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxFixedLengthTransactionSize = maxFixedLengthTransactionSize;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        config.isolationLevel = isolationLevel;
        config.writeSkewAllowed = isolationLevel.isWriteSkewAllowed();
        config.inconsistentReadAllowed = isolationLevel.isInconsistentReadAllowed();
        config.controlFlowErrorsReused = controlFlowErrorsReused;
        return config;
    }

    public GammaTransactionConfiguration setReadLockMode(LockMode readLockMode) {
        if (readLockMode == null) {
            throw new NullPointerException();
        }

        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.readLockMode = readLockMode;
        config.readLockModeAsInt = readLockMode.asInt();
        config.writeLockMode = writeLockMode;
        config.writeLockModeAsInt = writeLockMode.asInt();
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxFixedLengthTransactionSize = maxFixedLengthTransactionSize;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        config.isolationLevel = isolationLevel;
        config.writeSkewAllowed = isolationLevel.isWriteSkewAllowed();
        config.inconsistentReadAllowed = isolationLevel.isInconsistentReadAllowed();
        config.controlFlowErrorsReused = controlFlowErrorsReused;
        return config;
    }

    public GammaTransactionConfiguration addPermanentListener(TransactionListener listener) {
        if (listener == null) {
            throw new NullPointerException();
        }

        ArrayList<TransactionListener> newPermanentListeners
                = new ArrayList<TransactionListener>();
        if (permanentListeners != null) {
            newPermanentListeners.addAll(permanentListeners);
        }
        newPermanentListeners.add(listener);

        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.readLockMode = readLockMode;
        config.readLockModeAsInt = readLockModeAsInt;
        config.writeLockMode = writeLockMode;
        config.writeLockModeAsInt = writeLockModeAsInt;
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxFixedLengthTransactionSize = maxFixedLengthTransactionSize;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.isolationLevel = isolationLevel;
        config.writeSkewAllowed = isolationLevel.isWriteSkewAllowed();
        config.inconsistentReadAllowed = isolationLevel.isInconsistentReadAllowed();
        config.propagationLevel = propagationLevel;
        config.permanentListeners = newPermanentListeners;
        config.controlFlowErrorsReused = controlFlowErrorsReused;
        return config;
    }

    @Override
    public String toString() {
        return "GammaTransactionConfiguration{" +
                "familyName='" + familyName +
                ", propagationLevel=" + propagationLevel +
                ", isolationLevel=" + isolationLevel +
                ", readLockMode =" + readLockMode +
                ", writeLockMode =" + writeLockMode +
                ", traceLevel=" + traceLevel +
                ", readonly=" + readonly +
                ", speculativeConfiguration=" + speculativeConfiguration +
                ", spinCount=" + spinCount +
                ", dirtyCheck=" + dirtyCheck +
                ", minimalArrayTreeSize=" + minimalArrayTreeSize +
                ", trackReads=" + trackReads +
                ", maxRetries=" + maxRetries +
                ", speculativeConfigEnabled=" + speculativeConfigEnabled +
                ", maxArrayTransactionSize=" + maxFixedLengthTransactionSize +
                ", isAnonymous=" + isAnonymous +
                ", backoffPolicy=" + backoffPolicy +
                ", blockingAllowed=" + blockingAllowed +
                ", timeoutNs=" + timeoutNs +
                ", readWriteConflictReuse=" + controlFlowErrorsReused +
                ", interruptible=" + interruptible +
                ", permanentListeners=" + permanentListeners +
                '}';
    }

}
