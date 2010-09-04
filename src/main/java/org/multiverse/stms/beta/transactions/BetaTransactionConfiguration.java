package org.multiverse.stms.beta.transactions;

import org.multiverse.api.*;
import org.multiverse.api.exceptions.IllegalTransactionFactoryException;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.sensors.TransactionSensor;
import org.multiverse.stms.beta.BetaStm;
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
public final class BetaTransactionConfiguration implements TransactionConfiguration {

    public final static AtomicLong idGenerator = new AtomicLong();

    public final BetaStm stm;
    public final GlobalConflictCounter globalConflictCounter;
    private final AtomicReference<SpeculativeBetaConfig> speculativeConfig
            = new AtomicReference<SpeculativeBetaConfig>(new SpeculativeBetaConfig(true));
    public final TransactionSensor transactionSensor;
    public final StmCallback stmCallback;
    public final String familyName;
    public final boolean isAnonymous;
    public boolean interruptible = false;
    public boolean durable = false;
    public boolean readonly = false;
    public int spinCount = 16;
    public PessimisticLockLevel pessimisticLockLevel = PessimisticLockLevel.None;
    public boolean lockReads = false;
    public boolean lockWrites = false;
    public boolean dirtyCheck = true;
    public int minimalArrayTreeSize = 4;
    public boolean trackReads = true;
    public boolean blockingAllowed = true;
    public int maxRetries = 1000;
    public boolean speculativeConfigEnabled = true;
    public int maxArrayTransactionSize;
    public BackoffPolicy backoffPolicy = ExponentialBackoffPolicy.MAX_100_MS;
    public long timeoutNs = Long.MAX_VALUE;
    public TraceLevel traceLevel = TraceLevel.None;
    public boolean writeSkewAllowed = true;
    public PropagationLevel propagationLevel = PropagationLevel.Requires;

    public ArrayList<TransactionLifecycleListener> permanentListeners;

    public BetaTransactionConfiguration(BetaStm stm) {
        this(stm, stm.getMaxArrayTransactionSize());
    }

    public BetaTransactionConfiguration(BetaStm stm, String familyName, boolean isAnonymous) {
        if (stm == null) {
            throw new NullPointerException();
        }
        this.stm = stm;
        this.familyName = familyName;
        this.isAnonymous = isAnonymous;
        this.stmCallback = stm.getCallback();
        this.globalConflictCounter = stm.getGlobalConflictCounter();
        this.maxArrayTransactionSize = stm.getMaxArrayTransactionSize();
        this.transactionSensor = stm.getSimpleProfiler().getTransactionSensor(this);
    }

    public BetaTransactionConfiguration(BetaStm stm, int maxArrayTransactionSize) {
        if (stm == null) {
            throw new NullPointerException();
        }

        this.stm = stm;
        this.familyName = "anonymoustransaction-" + idGenerator.incrementAndGet();
        this.isAnonymous = true;
        this.globalConflictCounter = stm.getGlobalConflictCounter();
        this.maxArrayTransactionSize = maxArrayTransactionSize;
        this.stmCallback = stm.getCallback();
        this.transactionSensor = null;
    }

    public boolean hasTimeout() {
        return timeoutNs != Long.MAX_VALUE;
    }

    public SpeculativeBetaConfig getSpeculativeConfig() {
        return speculativeConfig.get();
    }

    @Override
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
            SpeculativeBetaConfig current = speculativeConfig.get();
            SpeculativeBetaConfig update = current.createWithOrElseRequired();
            if (speculativeConfig.compareAndSet(current, update)) {
                return;
            }
        }
    }

    public void needsListeners() {
        while (true) {
            SpeculativeBetaConfig current = speculativeConfig.get();
            SpeculativeBetaConfig update = current.createWithListenersRequired();
            if (speculativeConfig.compareAndSet(current, update)) {
                return;
            }
        }
    }

    public void needsCommute() {
        while (true) {
            SpeculativeBetaConfig current = speculativeConfig.get();
            SpeculativeBetaConfig update = current.createWithCommuteRequired();
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

    public void init() {
        if (!writeSkewAllowed && !trackReads && !readonly) {
            String msg = format("'[%s] If no writeskew is allowed, read tracking should be enabled", familyName);
            throw new IllegalTransactionFactoryException(msg);
        }

        if (blockingAllowed && !trackReads) {
            String msg = format("[%s] If blocking is allowed, read tracking should be enabled", familyName);
            throw new IllegalTransactionFactoryException(msg);
        }

        if (pessimisticLockLevel == PessimisticLockLevel.Read && !trackReads) {
            String msg = format("[%s] If all reads should be locked, read tracking should be enabled", familyName);
            throw new IllegalTransactionFactoryException(msg);
        }

        if (speculativeConfigEnabled) {
            boolean isFat = !writeSkewAllowed;

            if (speculativeConfig.get() == null) {
                SpeculativeBetaConfig newSpeculativeConfig = new SpeculativeBetaConfig(isFat);
                speculativeConfig.compareAndSet(null, newSpeculativeConfig);
            }
        }
    }

    public BetaTransactionConfiguration setTimeoutNs(long timeoutNs) {
        if (timeoutNs < 0) {
            throw new IllegalArgumentException("timeoutNs can't be smaller than 0");
        }

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.pessimisticLockLevel = pessimisticLockLevel;
        config.lockReads = pessimisticLockLevel.lockReads();
        config.lockWrites = pessimisticLockLevel.lockWrites();
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
        config.writeSkewAllowed = writeSkewAllowed;
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
        config.pessimisticLockLevel = pessimisticLockLevel;
        config.lockReads = pessimisticLockLevel.lockReads();
        config.lockWrites = pessimisticLockLevel.lockWrites();
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
        config.writeSkewAllowed = writeSkewAllowed;
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
        config.pessimisticLockLevel = pessimisticLockLevel;
        config.lockReads = pessimisticLockLevel.lockReads();
        config.lockWrites = pessimisticLockLevel.lockWrites();
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
        config.writeSkewAllowed = writeSkewAllowed;
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        return config;
    }

    public BetaTransactionConfiguration setReadTrackingEnabled(boolean trackReads) {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.pessimisticLockLevel = pessimisticLockLevel;
        config.lockReads = pessimisticLockLevel.lockReads();
        config.lockWrites = pessimisticLockLevel.lockWrites();
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
        config.writeSkewAllowed = writeSkewAllowed;
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        return config;
    }

    public BetaTransactionConfiguration setSpeculativeConfigurationEnabled(boolean speculativeConfigEnabled) {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.pessimisticLockLevel = pessimisticLockLevel;
        config.lockReads = pessimisticLockLevel.lockReads();
        config.lockWrites = pessimisticLockLevel.lockWrites();
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
        config.writeSkewAllowed = writeSkewAllowed;
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        return config;
    }

    public BetaTransactionConfiguration setReadonly(boolean readonly) {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.pessimisticLockLevel = pessimisticLockLevel;
        config.lockReads = pessimisticLockLevel.lockReads();
        config.lockWrites = pessimisticLockLevel.lockWrites();
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
        config.writeSkewAllowed = writeSkewAllowed;
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        return config;
    }

    public BetaTransactionConfiguration setDirtyCheckEnabled(boolean dirtyCheck) {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.pessimisticLockLevel = pessimisticLockLevel;
        config.lockReads = pessimisticLockLevel.lockReads();
        config.lockWrites = pessimisticLockLevel.lockWrites();
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
        config.writeSkewAllowed = writeSkewAllowed;
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        return config;
    }

    public BetaTransactionConfiguration setBlockingAllowed(boolean blockingEnabled) {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.pessimisticLockLevel = pessimisticLockLevel;
        config.lockReads = pessimisticLockLevel.lockReads();
        config.lockWrites = pessimisticLockLevel.lockWrites();
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
        config.writeSkewAllowed = writeSkewAllowed;
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        return config;
    }

    public BetaTransactionConfiguration setInterruptible(boolean interruptible) {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.pessimisticLockLevel = pessimisticLockLevel;
        config.lockReads = pessimisticLockLevel.lockReads();
        config.lockWrites = pessimisticLockLevel.lockWrites();
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
        config.writeSkewAllowed = writeSkewAllowed;
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
        config.pessimisticLockLevel = pessimisticLockLevel;
        config.lockReads = pessimisticLockLevel.lockReads();
        config.lockWrites = pessimisticLockLevel.lockWrites();
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
        config.writeSkewAllowed = writeSkewAllowed;
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        return config;
    }

    public BetaTransactionConfiguration setPessimisticLockLevel(PessimisticLockLevel pessimisticLockLevel) {
        if (pessimisticLockLevel == null) {
            throw new NullPointerException("pessimisticLockLevel can't be null");
        }

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.pessimisticLockLevel = pessimisticLockLevel;
        config.lockReads = pessimisticLockLevel.lockReads();
        config.lockWrites = pessimisticLockLevel.lockWrites();
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
        config.writeSkewAllowed = writeSkewAllowed;
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
        config.pessimisticLockLevel = pessimisticLockLevel;
        config.lockReads = pessimisticLockLevel.lockReads();
        config.lockWrites = pessimisticLockLevel.lockWrites();
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
        config.writeSkewAllowed = writeSkewAllowed;
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
        config.pessimisticLockLevel = pessimisticLockLevel;
        config.lockReads = pessimisticLockLevel.lockReads();
        config.lockWrites = pessimisticLockLevel.lockWrites();
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
        config.writeSkewAllowed = writeSkewAllowed;
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
        return config;
    }

    public BetaTransactionConfiguration setWriteSkewAllowed(boolean writeSkewAllowed) {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, familyName, isAnonymous);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.pessimisticLockLevel = pessimisticLockLevel;
        config.lockReads = pessimisticLockLevel.lockReads();
        config.lockWrites = pessimisticLockLevel.lockWrites();
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
        config.writeSkewAllowed = writeSkewAllowed;
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
        config.pessimisticLockLevel = pessimisticLockLevel;
        config.lockReads = pessimisticLockLevel.lockReads();
        config.lockWrites = pessimisticLockLevel.lockWrites();
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
        config.writeSkewAllowed = writeSkewAllowed;
        config.propagationLevel = propagationLevel;
        config.permanentListeners = permanentListeners;
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
        config.pessimisticLockLevel = pessimisticLockLevel;
        config.lockReads = pessimisticLockLevel.lockReads();
        config.lockWrites = pessimisticLockLevel.lockWrites();
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
        config.writeSkewAllowed = writeSkewAllowed;
        config.propagationLevel = propagationLevel;
        config.permanentListeners = newPermanentListeners;
        return config;
    }

    @Override
    public String toString() {
        return "BetaTransactionConfiguration{" +
                "familyName='" + familyName +
                ", interruptible=" + interruptible +
                ", durable=" + durable +
                ", readonly=" + readonly +
                ", spinCount=" + spinCount +
                ", lockReads=" + lockReads +
                ", lockWrites=" + lockWrites +
                ", pessimisticLockLevel=" + pessimisticLockLevel +
                ", dirtyCheck=" + dirtyCheck +
                ", stm=" + stm +
                ", globalConflictCounter=" + globalConflictCounter +
                ", minimalArrayTreeSize=" + minimalArrayTreeSize +
                ", trackReads=" + trackReads +
                ", blockingAllowed=" + blockingAllowed +
                ", maxRetries=" + maxRetries +
                ", speculativeConfigEnabled=" + speculativeConfigEnabled +
                ", maxArrayTransactionSize=" + maxArrayTransactionSize +
                ", isAnonymous=" + isAnonymous +
                ", backoffPolicy=" + backoffPolicy +
                ", timeoutNs=" + timeoutNs +
                ", traceLevel=" + traceLevel +
                ", writeSkewAllowed=" + writeSkewAllowed +
                ", propagationLevel=" + propagationLevel +
                ", speculativeConfig=" + speculativeConfig +
                ", permanentListeners=" + permanentListeners +
                '}';
    }
}
