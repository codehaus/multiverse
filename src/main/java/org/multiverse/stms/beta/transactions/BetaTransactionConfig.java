package org.multiverse.stms.beta.transactions;

import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.conflictcounters.GlobalConflictCounter;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Configuration for the BetaTransaction. Should not be changed from the outside.
 *
 * @author Peter Veentjer
 */
public final class BetaTransactionConfig {

    public final static AtomicLong idGenerator = new AtomicLong();

    public boolean durable = false;
    public boolean readonly = false;
    public int spinCount = 16;
    public boolean lockReads = false;
    public boolean lockWrites = false;
    public PessimisticLockLevel pessimisticLockLevel = PessimisticLockLevel.None;
    public boolean dirtyCheck = true;
    public final BetaStm stm;
    public final GlobalConflictCounter globalConflictCounter;
    public int maxLinearSearch = 4;
    public boolean trackReads = true;
    public boolean explicitRetryAllowed = true;
    public int maxRetries = 1000;
    public String familyName = "anonymoustransaction-" + idGenerator.incrementAndGet();

    public BetaTransactionConfig(BetaStm stm) {
        this.stm = stm;
        this.globalConflictCounter = stm.getGlobalConflictCounter();
    }

    public String getFamilyName() {
        return familyName;
    }

    public boolean isReadonly() {
        return readonly;
    }

    public int getSpinCount() {
        return spinCount;
    }

    public boolean isLockReads() {
        return lockReads;
    }

    public boolean isLockWrites() {
        return lockWrites;
    }

    public PessimisticLockLevel getPessimisticLockLevel() {
        return pessimisticLockLevel;
    }

    public boolean isDirtyCheck() {
        return dirtyCheck;
    }

    public BetaStm getStm() {
        return stm;
    }

    public GlobalConflictCounter getGlobalConflictCounter() {
        return globalConflictCounter;
    }

    public int getMaxLinearSearch() {
        return maxLinearSearch;
    }

    public boolean isTrackReads() {
        return trackReads;
    }

    public boolean isExplicitRetryAllowed() {
        return explicitRetryAllowed;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public BetaTransactionConfig setFamilyName(String familyName) {
        if (familyName == null) {
            throw new NullPointerException("familyName can't be smaller than 0");
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
        config.explicitRetryAllowed = explicitRetryAllowed;
        config.familyName = familyName;
        config.durable = durable;
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
        config.explicitRetryAllowed = explicitRetryAllowed;
        config.familyName = familyName;
        config.durable = durable;
        return config;
    }

    public BetaTransactionConfig setTrackReadsEnabled(boolean trackReads) {
        BetaTransactionConfig config = new BetaTransactionConfig(stm);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.pessimisticLockLevel = pessimisticLockLevel;
        config.lockReads = pessimisticLockLevel.lockReads();
        config.lockWrites = pessimisticLockLevel.lockWrites();
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.explicitRetryAllowed = explicitRetryAllowed;
        config.familyName = familyName;
        config.durable = durable;
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
        config.explicitRetryAllowed = explicitRetryAllowed;
        config.familyName = familyName;
        config.durable = durable;
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
        config.explicitRetryAllowed = explicitRetryAllowed;
        config.familyName = familyName;
        config.durable = durable;
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
        config.explicitRetryAllowed = explicitRetryAllowed;
        config.familyName = familyName;
        config.durable = durable;
        return config;
    }

    public BetaTransactionConfig setExplicitRetryAllowed(boolean explicitRetryAllowed) {
        BetaTransactionConfig config = new BetaTransactionConfig(stm);
        config.readonly = readonly;
        config.spinCount = spinCount;
        config.pessimisticLockLevel = pessimisticLockLevel;
        config.lockReads = pessimisticLockLevel.lockReads();
        config.lockWrites = pessimisticLockLevel.lockWrites();
        config.dirtyCheck = dirtyCheck;
        config.trackReads = trackReads;
        config.maxRetries = maxRetries;
        config.explicitRetryAllowed = explicitRetryAllowed;
        config.familyName = familyName;
        config.durable = durable;
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
        config.explicitRetryAllowed = explicitRetryAllowed;
        config.familyName = familyName;
        config.durable = durable;
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
        config.explicitRetryAllowed = explicitRetryAllowed;
        config.familyName = familyName;
        config.durable = durable;
        return config;
    }
}
