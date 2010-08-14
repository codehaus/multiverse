package org.multiverse.stms.beta.transactions;

import org.multiverse.api.*;
import org.multiverse.api.exceptions.IllegalTransactionFactoryException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.conflictcounters.GlobalConflictCounter;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.String.format;

/**
 * Configuration for the BetaTransaction. Should not be changed from the outside.
 *
 * @author Peter Veentjer
 */
public final class BetaTransactionConfig implements TransactionConfiguration {

    public final static AtomicLong idGenerator = new AtomicLong();

    public boolean interruptible = false;
    public boolean durable = false;
    public boolean readonly = false;
    public int spinCount = 16;
    public boolean lockReads = false;
    public boolean lockWrites = false;
    public PessimisticLockLevel pessimisticLockLevel = PessimisticLockLevel.None;
    public boolean dirtyCheck = true;
    public final BetaStm stm;
    public final GlobalConflictCounter globalConflictCounter;
    public int minimalArrayTreeSize = 4;
    public boolean trackReads = true;
    public boolean blockingAllowed = true;
    public int maxRetries = 1000;
    public String familyName = "anonymoustransaction-" + idGenerator.incrementAndGet();
    public boolean speculativeConfigEnabled = true;
    public int maxArrayTransactionSize;
    public boolean isAnonymous = true;
    public BackoffPolicy backoffPolicy = BackoffPolicy.INSTANCE_100_MS_MAX;
    public long timeoutNs = Long.MAX_VALUE;
    public TraceLevel traceLevel = TraceLevel.None;
    public boolean writeSkewAllowed = true;
    public PropagationLevel propagationLevel = PropagationLevel.Requires;
    private final AtomicReference<SpeculativeBetaConfig> speculativeConfig
            = new AtomicReference<SpeculativeBetaConfig>(new SpeculativeBetaConfig());

    public BetaTransactionConfig(BetaStm stm) {
        this.stm = stm;
        this.globalConflictCounter = stm.getGlobalConflictCounter();
        this.maxArrayTransactionSize = stm.getMaxArrayTransactionSize();
    }

    public BetaTransactionConfig(BetaStm stm, int maxArrayTransactionSize) {
        this.stm = stm;
        this.globalConflictCounter = stm.getGlobalConflictCounter();
        this.maxArrayTransactionSize = maxArrayTransactionSize;
    }

    public boolean hasTimeout() {
        return timeoutNs != Long.MAX_VALUE;
    }

    public SpeculativeBetaConfig getSpeculativeConfig() {
        return speculativeConfig.get();
    }

    public long getTimeoutNs() {
        return timeoutNs;
    }

    @Override
    public boolean isWriteSkewAllowed() {
        return writeSkewAllowed;
    }

    @Override
    public TraceLevel getTraceLevel() {
        return traceLevel;
    }

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
    public boolean isLockReads() {
        return lockReads;
    }

    @Override
    public boolean isLockWrites() {
        return lockWrites;
    }

    @Override
    public PessimisticLockLevel getPessimisticLockLevel() {
        return pessimisticLockLevel;
    }

    @Override
    public boolean isDirtyCheck() {
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
    public int getMinimalArrayTreeSize() {
        return minimalArrayTreeSize;
    }

    @Override
    public boolean isTrackReads() {
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

    public void needsListeners() {
        while (true) {
            SpeculativeBetaConfig current = speculativeConfig.get();
            SpeculativeBetaConfig update = current.createWithListenersEnabled();
            if (speculativeConfig.compareAndSet(current, update)) {
                return;
            }
        }
    }

    public void needsMinimalTransactionLength(int newLength) {
        while (true) {
            SpeculativeBetaConfig current = speculativeConfig.get();
            SpeculativeBetaConfig next = current.createWithMinimalLength(newLength);

            if (speculativeConfig.compareAndSet(current, next)) {
                return;
            }
        }
    }

    public void validate() {
        if (!writeSkewAllowed && !trackReads && !readonly) {
            String msg = format("'%s': If no writeskew is allowed, read tracking should be enabled", familyName);
            throw new IllegalTransactionFactoryException(msg);
        }

        if (blockingAllowed && !trackReads) {
            String msg = format("'%s': If blocking is allowed, read tracking should be enabled", familyName);
            throw new IllegalTransactionFactoryException(msg);
        }

        if (pessimisticLockLevel == PessimisticLockLevel.Read && !trackReads) {
            String msg = format("'%s': If all reads are locked, read tracking should be enabled", familyName);
            throw new IllegalTransactionFactoryException(msg);
        }
    }

    public BetaTransactionConfig setTimeoutNs(long timeoutNs) {
        if (timeoutNs < 0) {
            throw new IllegalArgumentException("timeoutNs can't be smaller than 0");
        }

        BetaTransactionConfig config = new BetaTransactionConfig(stm);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.pessimisticLockLevel = pessimisticLockLevel;
        config.lockReads = pessimisticLockLevel.lockReads();
        config.lockWrites = pessimisticLockLevel.lockWrites();
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.familyName = familyName;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxArrayTransactionSize = maxArrayTransactionSize;
        config.isAnonymous = false;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.writeSkewAllowed = writeSkewAllowed;
        config.propagationLevel = propagationLevel;
        return config;
    }

    public BetaTransactionConfig setFamilyName(String familyName) {
        if (familyName == null) {
            throw new NullPointerException("familyName can't be null");
        }

        BetaTransactionConfig config = new BetaTransactionConfig(stm);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.pessimisticLockLevel = pessimisticLockLevel;
        config.lockReads = pessimisticLockLevel.lockReads();
        config.lockWrites = pessimisticLockLevel.lockWrites();
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.familyName = familyName;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxArrayTransactionSize = maxArrayTransactionSize;
        config.isAnonymous = false;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.writeSkewAllowed = writeSkewAllowed;
        config.propagationLevel = propagationLevel;
        return config;
    }

    public BetaTransactionConfig setMaxRetries(int maxRetries) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries can't be smaller than 0");
        }

        BetaTransactionConfig config = new BetaTransactionConfig(stm);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.pessimisticLockLevel = pessimisticLockLevel;
        config.lockReads = pessimisticLockLevel.lockReads();
        config.lockWrites = pessimisticLockLevel.lockWrites();
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.familyName = familyName;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxArrayTransactionSize = maxArrayTransactionSize;
        config.isAnonymous = isAnonymous;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.writeSkewAllowed = writeSkewAllowed;
        config.propagationLevel = propagationLevel;
        return config;
    }

    public BetaTransactionConfig setReadTrackingEnabled(boolean trackReads) {
        BetaTransactionConfig config = new BetaTransactionConfig(stm);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.pessimisticLockLevel = pessimisticLockLevel;
        config.lockReads = pessimisticLockLevel.lockReads();
        config.lockWrites = pessimisticLockLevel.lockWrites();
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.familyName = familyName;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxArrayTransactionSize = maxArrayTransactionSize;
        config.isAnonymous = isAnonymous;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.writeSkewAllowed = writeSkewAllowed;
        config.propagationLevel = propagationLevel;
        return config;
    }

    public BetaTransactionConfig setSpeculativeConfigurationEnabled(boolean speculativeConfigEnabled) {
        BetaTransactionConfig config = new BetaTransactionConfig(stm);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.pessimisticLockLevel = pessimisticLockLevel;
        config.lockReads = pessimisticLockLevel.lockReads();
        config.lockWrites = pessimisticLockLevel.lockWrites();
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.familyName = familyName;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxArrayTransactionSize = maxArrayTransactionSize;
        config.isAnonymous = isAnonymous;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.writeSkewAllowed = writeSkewAllowed;
        config.propagationLevel = propagationLevel;
        return config;
    }

    public BetaTransactionConfig setReadonly(boolean readonly) {
        BetaTransactionConfig config = new BetaTransactionConfig(stm);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.pessimisticLockLevel = pessimisticLockLevel;
        config.lockReads = pessimisticLockLevel.lockReads();
        config.lockWrites = pessimisticLockLevel.lockWrites();
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.familyName = familyName;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxArrayTransactionSize = maxArrayTransactionSize;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.writeSkewAllowed = writeSkewAllowed;
        config.propagationLevel = propagationLevel;
        return config;
    }

    public BetaTransactionConfig setDurable(boolean durable) {
        BetaTransactionConfig config = new BetaTransactionConfig(stm);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.pessimisticLockLevel = pessimisticLockLevel;
        config.lockReads = pessimisticLockLevel.lockReads();
        config.lockWrites = pessimisticLockLevel.lockWrites();
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.familyName = familyName;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxArrayTransactionSize = maxArrayTransactionSize;
        config.isAnonymous = isAnonymous;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.writeSkewAllowed = writeSkewAllowed;
        config.propagationLevel = propagationLevel;
        return config;
    }

    public BetaTransactionConfig setDirtyCheckEnabled(boolean dirtyCheck) {
        BetaTransactionConfig config = new BetaTransactionConfig(stm);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.pessimisticLockLevel = pessimisticLockLevel;
        config.lockReads = pessimisticLockLevel.lockReads();
        config.lockWrites = pessimisticLockLevel.lockWrites();
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.familyName = familyName;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxArrayTransactionSize = maxArrayTransactionSize;
        config.isAnonymous = isAnonymous;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.writeSkewAllowed = writeSkewAllowed;
        config.propagationLevel = propagationLevel;
        return config;
    }

    public BetaTransactionConfig setBlockingAllowed(boolean blockingEnabled) {
        BetaTransactionConfig config = new BetaTransactionConfig(stm);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.pessimisticLockLevel = pessimisticLockLevel;
        config.lockReads = pessimisticLockLevel.lockReads();
        config.lockWrites = pessimisticLockLevel.lockWrites();
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingEnabled;
        config.familyName = familyName;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxArrayTransactionSize = maxArrayTransactionSize;
        config.isAnonymous = isAnonymous;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.writeSkewAllowed = writeSkewAllowed;
        config.propagationLevel = propagationLevel;
        return config;
    }

    public BetaTransactionConfig setInterruptible(boolean interruptible) {
        BetaTransactionConfig config = new BetaTransactionConfig(stm);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.pessimisticLockLevel = pessimisticLockLevel;
        config.lockReads = pessimisticLockLevel.lockReads();
        config.lockWrites = pessimisticLockLevel.lockWrites();
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.familyName = familyName;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxArrayTransactionSize = maxArrayTransactionSize;
        config.isAnonymous = isAnonymous;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.writeSkewAllowed = writeSkewAllowed;
        config.propagationLevel = propagationLevel;
        return config;
    }

    public BetaTransactionConfig setSpinCount(int spinCount) {
        if (spinCount < 0) {
            throw new IllegalArgumentException("spinCount can't be smaller than 0");
        }

        BetaTransactionConfig config = new BetaTransactionConfig(stm);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.pessimisticLockLevel = pessimisticLockLevel;
        config.lockReads = pessimisticLockLevel.lockReads();
        config.lockWrites = pessimisticLockLevel.lockWrites();
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.familyName = familyName;
        config.durable = durable;
        config.maxArrayTransactionSize = maxArrayTransactionSize;
        config.isAnonymous = isAnonymous;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.writeSkewAllowed = writeSkewAllowed;
        config.propagationLevel = propagationLevel;
        return config;
    }

    public BetaTransactionConfig setPessimisticLockLevel(PessimisticLockLevel pessimisticLockLevel) {
        if (pessimisticLockLevel == null) {
            throw new NullPointerException("pessimisticLockLevel can't be null");
        }

        BetaTransactionConfig config = new BetaTransactionConfig(stm);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.pessimisticLockLevel = pessimisticLockLevel;
        config.lockReads = pessimisticLockLevel.lockReads();
        config.lockWrites = pessimisticLockLevel.lockWrites();
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.familyName = familyName;
        config.durable = durable;
        config.maxArrayTransactionSize = maxArrayTransactionSize;
        config.isAnonymous = isAnonymous;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.writeSkewAllowed = writeSkewAllowed;
        config.propagationLevel = propagationLevel;
        return config;
    }

    public BetaTransactionConfig setBackoffPolicy(BackoffPolicy backoffPolicy) {
        if (backoffPolicy == null) {
            throw new NullPointerException("backoffPolicy can't be null");
        }

        BetaTransactionConfig config = new BetaTransactionConfig(stm);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.pessimisticLockLevel = pessimisticLockLevel;
        config.lockReads = pessimisticLockLevel.lockReads();
        config.lockWrites = pessimisticLockLevel.lockWrites();
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.familyName = familyName;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxArrayTransactionSize = maxArrayTransactionSize;
        config.isAnonymous = false;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.writeSkewAllowed = writeSkewAllowed;
        config.propagationLevel = propagationLevel;
        return config;
    }

    public BetaTransactionConfig setTraceLevel(TraceLevel traceLevel) {
        if (traceLevel == null) {
            throw new NullPointerException("traceLevel can't be null");
        }

        BetaTransactionConfig config = new BetaTransactionConfig(stm);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.pessimisticLockLevel = pessimisticLockLevel;
        config.lockReads = pessimisticLockLevel.lockReads();
        config.lockWrites = pessimisticLockLevel.lockWrites();
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.familyName = familyName;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxArrayTransactionSize = maxArrayTransactionSize;
        config.isAnonymous = false;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.writeSkewAllowed = writeSkewAllowed;
        config.propagationLevel = propagationLevel;
        return config;
    }

    public BetaTransactionConfig setWriteSkewAllowed(boolean writeSkewAllowed) {
        BetaTransactionConfig config = new BetaTransactionConfig(stm);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.pessimisticLockLevel = pessimisticLockLevel;
        config.lockReads = pessimisticLockLevel.lockReads();
        config.lockWrites = pessimisticLockLevel.lockWrites();
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.familyName = familyName;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxArrayTransactionSize = maxArrayTransactionSize;
        config.isAnonymous = false;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.writeSkewAllowed = writeSkewAllowed;
        config.propagationLevel = propagationLevel;
        return config;
    }

    public BetaTransactionConfig setPropagationLevel(PropagationLevel propagationLevel) {
        if (propagationLevel == null) {
            throw new NullPointerException();
        }

        BetaTransactionConfig config = new BetaTransactionConfig(stm);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.pessimisticLockLevel = pessimisticLockLevel;
        config.lockReads = pessimisticLockLevel.lockReads();
        config.lockWrites = pessimisticLockLevel.lockWrites();
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.blockingAllowed = blockingAllowed;
        config.familyName = familyName;
        config.durable = durable;
        config.speculativeConfigEnabled = speculativeConfigEnabled;
        config.maxArrayTransactionSize = maxArrayTransactionSize;
        config.isAnonymous = false;
        config.backoffPolicy = backoffPolicy;
        config.interruptible = interruptible;
        config.timeoutNs = timeoutNs;
        config.traceLevel = traceLevel;
        config.writeSkewAllowed = writeSkewAllowed;
        config.propagationLevel = propagationLevel;
        return config;
    }
}
