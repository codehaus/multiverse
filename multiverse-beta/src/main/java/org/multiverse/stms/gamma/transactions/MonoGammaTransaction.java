package org.multiverse.stms.gamma.transactions;

import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaObject;
import org.multiverse.stms.gamma.transactionalobjects.GammaTranlocal;

public final class MonoGammaTransaction extends GammaTransaction {

    public final GammaTranlocal tranlocal = new GammaTranlocal();

    public MonoGammaTransaction(GammaStm stm) {
        this(new GammaTransactionConfiguration(stm));
    }

    public MonoGammaTransaction(GammaTransactionConfiguration config) {
        super(config, POOL_TRANSACTIONTYPE_MONO);
    }

    @Override
    public void commute(GammaLongRef ref, LongFunction function) {
        throw new TodoException();
    }

    @Override
    public GammaTranlocal openForWrite(GammaLongRef o, int lockMode) {
        return o.openForWrite(this, lockMode);
    }

    @Override
    public GammaTranlocal openForRead(GammaLongRef o, int lockMode) {
        return o.openForRead(this, lockMode);
    }

    @Override
    public GammaTranlocal locate(GammaObject o) {
        if (status != TX_ACTIVE) {
            throw abortLocateOnBadStatus(o);
        }

        if (o == null) {
            throw abortLocateOnNullArgument();
        }

        return get(o);
    }

    @Override
    public void commit() {
        if (status == TX_COMMITTED) {
            return;
        }

        if (status != TX_ACTIVE && status != TX_PREPARED) {
            throw abortCommitOnBadStatus();
        }

        if (abortOnly) {
            //throw new AbortOn
        }

        GammaObject owner = tranlocal.owner;

        if (owner != null) {
            if (tranlocal.mode == TRANLOCAL_READ) {
                owner.releaseAfterReading(tranlocal, pool);
            } else if (tranlocal.mode == TRANLOCAL_WRITE) {
                GammaLongRef ref = (GammaLongRef) owner;

                if (status == TX_ACTIVE) {
                    if (!tranlocal.isDirty()) {
                        boolean isDirty = tranlocal.long_value != tranlocal.long_oldValue;

                        if (isDirty) {
                            tranlocal.setDirty(true);
                        }
                    }

                    if (!ref.tryLockAndCheckConflict(config.spinCount, tranlocal, LOCKMODE_COMMIT)) {
                        throw abortOnReadWriteConflict();
                    }


                    //if (!tranlocal.prepare(config)) {
                    //
                    //}
                }

                if (tranlocal.isDirty()) {
                    ref.version = tranlocal.version + 1;
                    ref.value = tranlocal.long_value;
                    //ref.releaseAfterUpdate(tranlocal, pool);
                    ref.departAfterUpdateAndUnlock();
                    tranlocal.setLockMode(LOCKMODE_NONE);
                    tranlocal.owner = null;
                    tranlocal.setDepartObligation(false);
                } else {
                    owner.releaseAfterReading(tranlocal, pool);
                }
            } else {
                throw new TodoException();
            }
        }

        status = TX_COMMITTED;
        tranlocal.owner = null;
    }

    @Override
    public void abort() {
        if (status == TX_ABORTED) {
            return;
        }

        if (status == TX_COMMITTED) {
            throw new DeadTransactionException();
        }

        status = TX_ABORTED;
        GammaObject owner = tranlocal.owner;
        if (owner != null) {
            owner.releaseAfterFailure(tranlocal, pool);
        }
    }

    @Override
    public void prepare() {
        if (status == TX_PREPARED) {
            return;
        }

        if (status != TX_ACTIVE) {
            throw abortPrepareOnBadStatus();
        }

        if (tranlocal.owner != null) {
            if (!tranlocal.prepare(config)) {
                throw abortOnReadWriteConflict();
            }
        }

        status = TX_PREPARED;
    }

    @Override
    public GammaTranlocal get(GammaObject ref) {
        return tranlocal.owner == ref ? tranlocal : null;
    }

    @Override
    public void retry() {
        if (status != TX_ACTIVE && status != TX_PREPARED) {
            throw abortRetryOnBadStatus();
        }

        if (!config.isBlockingAllowed()) {
            throw abortRetryOnNoBlockingAllowed();
        }

        throw new TodoException();
    }

    @Override
    public boolean softReset() {
        if(attempt >= config.getMaxRetries()){
            return false;
        }

        status = TX_ACTIVE;
        hasWrites = false;
        attempt++;
        abortOnly = false;
        return true;
    }

    public void hardReset() {
        status = TX_ACTIVE;
        hasWrites = false;
        remainingTimeoutNs = config.timeoutNs;
        attempt = 0;
        abortOnly = false;
    }

    public boolean isReadConsistent(GammaTranlocal justAdded){
        return true;
    }

    @Override
    public void copyForSpeculativeFailure(GammaTransaction failingTx) {
        throw new TodoException();
    }
}
