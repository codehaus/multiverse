package org.multiverse.stms.beta.transactionalobjects;

import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.api.functions.Function;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.BetaTransactionConfiguration;

/**
 * The BetaTranlocal contains the transaction local state of a BetaTransactionalObject (so also the refs).
 * It only exists when a transaction is running, and when it commits, the values it contains will be
 * written to the BetaTransactionalObject if needed.
 *
 * @author Peter Veentjer
 */
public abstract class BetaTranlocal implements BetaStmConstants {

    public long version;
    public BetaTransaction tx;
    public BetaTransactionalObject owner;
    public CallableNode headCallable;

    public int status = STATUS_NEW;
    private int lockMode;

    private boolean checkConflict;
    private boolean hasDepartObligation;
    private boolean isDirty;
    private boolean ignore;

    public BetaTranlocal(BetaTransactionalObject owner) {
        this.owner = owner;
    }

    public final boolean isNew() {
        return status == STATUS_NEW;
    }

    public final boolean isConstructing() {
        return status == STATUS_CONSTRUCTING;
    }

    public final void setStatus(int state) {
        this.status = state;
    }

    public final boolean isReadonly() {
        return status == STATUS_READONLY;
    }

    public final boolean isNew(int status) {
        return status == STATUS_NEW;
    }

    public final boolean isConstructing(int status) {
        return status == STATUS_CONSTRUCTING;
    }

    public final boolean isReadonly(int status) {
        return status == STATUS_READONLY;
    }

    public final boolean isConflictCheckNeeded() {
        return checkConflict;
    }

    public final void setIsConflictCheckNeeded(boolean value) {
        checkConflict = value;
    }

    public final int getLockMode() {
        return lockMode;
    }

    public final void setLockMode(int lockMode) {
        this.lockMode = lockMode;
    }

    public final boolean isDirty() {
        return isDirty;
    }

    public final void setDirty(boolean value) {
        this.isDirty = value;
    }

    public final boolean isCommuting() {
        return status == STATUS_COMMUTING;
    }

    public final boolean hasDepartObligation() {
        return hasDepartObligation;
    }

    public final void setDepartObligation(boolean value) {
        hasDepartObligation = value;
    }

    public abstract void prepareForPooling(final BetaObjectPool pool);

    public boolean ignore() {
        return ignore;
    }

    public void setIgnore(boolean value) {
        this.ignore = value;
    }

    public final void openForCommute() {
        if (tx.status != BetaTransaction.ACTIVE) {
            //todo: function needs to be set.
            throw tx.abortCommute(owner, null);
        }
    }

    public final void openForConstruction() {
        if (tx.status != BetaTransaction.ACTIVE) {
            throw tx.abortOpenForConstruction(owner);
        }

        switch (status) {
            case STATUS_NEW: {
                if (tx.config.readonly) {
                    throw tx.abortOpenForWriteWhenReadonly(owner);
                }

                status = STATUS_CONSTRUCTING;
                return;
            }
            case STATUS_CONSTRUCTING: {
                return;
            }
            case STATUS_COMMUTING: {
                throw new IllegalStateException();
            }
            case STATUS_UPDATE: {
                throw new IllegalStateException();
            }
            case STATUS_READONLY: {
                throw new IllegalStateException();
            }
            default:
                throw new IllegalStateException();
        }
    }

    public abstract void openForRead(int desiredLockMode);

    public abstract void openForWrite(int desiredLockMode);

    public final void upgradeLockMode(int desiredLockMode) {
        if (tx.status != BetaTransaction.ACTIVE) {
            //todo: make use of the correct abort method
            throw tx.abortOpenForWrite(owner);
        }

        //desiredLockMode = desiredLockMode >= config.writeLockMode
        //               ? desiredLockMode
        //               : config.writeLockMode;
        //

        if (lockMode >= desiredLockMode) {
            return;
        }

        if (!isCommuting()) {
            //todo
        }

        final BetaTransactionConfiguration config = tx.config;

        final boolean lockSuccess = owner.___tryLockAndCheckConflict(
                tx, config.spinCount, this, desiredLockMode == LOCKMODE_COMMIT);

        if (!lockSuccess) {
            throw ReadWriteConflict.INSTANCE;
        }
    }

    public final void checkConflict() {
        if (tx.status != BetaTransaction.ACTIVE) {
            //todo: make use of the correct abort method
            throw tx.abortOpenForWrite(owner);
        }

        if (lockMode != LOCKMODE_NONE) {
            return;
        }

        if (checkConflict) {
            return;
        }

        checkConflict = true;
    }

    /**
     * Calculates if this BetaTranlocal is dirty (so needs to be written) and stores the result in the
     * isDirty field. The call can be made more than once, but once it is marked as dirty, it will remain
     * dirty.
     *
     * @return true if dirty, false otherwise.
     */
    public abstract boolean calculateIsDirty();

    /**
     * Evaluates the commuting functions that are applied to this BetaTranlocal. This call is made under the
     * assumption that the BetaTranlocal is not committed, is in the 'isCommuting' mode and that the read
     * field has been getAndSet. If there is a change, the isDirty field also is getAndSet.
     *
     * @param pool the BetaObjectPool used to pool resources.
     */
    public abstract void evaluateCommutingFunctions(BetaObjectPool pool);

    /**
     * Adds a Function for commute to this BetaTranlocal. This call is made under the assumption that
     * the BetaTranlocal is not committed and in the 'isCommuting' mode.
     * <p/>
     * No checks on the Function are done, so no null check or check if the Function already is added.
     *
     * @param function the Function to add.
     * @param pool     the BetaObjectPool that can be used to pool resources for this operation.
     */
    public abstract void addCommutingFunction(Function function, BetaObjectPool pool);

    public final boolean prepareDirtyUpdates(
            final BetaObjectPool pool, final BetaTransaction tx, final int spinCount) {

        switch (status) {
            case STATUS_NEW:
                throw new IllegalStateException();
            case STATUS_CONSTRUCTING:
                return true;
            case STATUS_COMMUTING:
                if (!owner.___load(spinCount, tx, LOCKMODE_COMMIT, this)) {
                    return false;
                }

                evaluateCommutingFunctions(pool);
                return true;
            case STATUS_READONLY:
                if (!checkConflict) {
                    return true;
                }

                if (lockMode != LOCKMODE_NONE) {
                    return true;
                }

                return owner.___tryLockAndCheckConflict(tx, spinCount, this, false);
            case STATUS_UPDATE:
                if (!(isDirty || calculateIsDirty())) {

                    if (!checkConflict) {
                        return true;
                    }

                    if (lockMode != LOCKMODE_NONE) {
                        return true;
                    }

                    return owner.___tryLockAndCheckConflict(tx, spinCount, this, false);
                }

                if (lockMode == LOCKMODE_COMMIT) {
                    return true;
                }

                return owner.___tryLockAndCheckConflict(tx, spinCount, this, true);
            default:
                throw new IllegalStateException();
        }
    }

    public final boolean prepareAllUpdates(
            final BetaObjectPool pool, final BetaTransaction tx, final int spinCount) {

        switch (status) {
            case STATUS_NEW:
                throw new IllegalStateException();
            case STATUS_CONSTRUCTING:
                return true;
            case STATUS_COMMUTING:
                if (!owner.___load(spinCount, tx, LOCKMODE_COMMIT, this)) {
                    return false;
                }

                evaluateCommutingFunctions(pool);
                return true;
            case STATUS_READONLY:
                if (!checkConflict) {
                    return true;
                }

                if (lockMode != LOCKMODE_NONE) {
                    return true;
                }

                return owner.___tryLockAndCheckConflict(tx, spinCount, this, false);
            case STATUS_UPDATE:
                if (lockMode == LOCKMODE_COMMIT) {
                    return true;
                }

                return owner.___tryLockAndCheckConflict(tx, spinCount, this, true);
            default:
                throw new IllegalStateException();
        }
    }
}
