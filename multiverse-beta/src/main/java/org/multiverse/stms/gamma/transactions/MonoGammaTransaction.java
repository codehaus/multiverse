package org.multiverse.stms.gamma.transactions;

import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.TodoException;
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
        super(config);
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
            throw abortLocateOnBadStatus();
        }

        if (o == null) {
            throw abortLocateOnNullArgument();
        }

        return get(o);
    }

    @Override
    public void commit() {
        if(status == TX_COMMITTED){
            return;
        }

        if (status != TX_ACTIVE && status != TX_PREPARED) {
            throw abortCommitOnBadStatus();
        }

        GammaObject owner = tranlocal.owner;

        if (owner != null) {
            if (tranlocal.mode == TRANLOCAL_READ) {
                owner.releaseAfterReading(tranlocal, pool);
            } else if (tranlocal.mode == TRANLOCAL_WRITE) {
                if (status == TX_ACTIVE) {
                    if (!tranlocal.prepare(config)) {
                        throw abortOnReadWriteConflict();
                    }
                }

                if (tranlocal.isDirty()) {
                    GammaLongRef ref = (GammaLongRef) owner;
                    ref.version++;
                    ref.value = tranlocal.long_value;
                    ref.releaseAfterUpdate(tranlocal, pool);
                 }else{
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


    public void reset() {
        status = TX_ACTIVE;
        hasWrites = false;
        remainingTimeoutNs = config.timeoutNs;
        attempt = 0;
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

}
