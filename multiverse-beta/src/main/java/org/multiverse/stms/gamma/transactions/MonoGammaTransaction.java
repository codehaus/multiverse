package org.multiverse.stms.gamma.transactions;

import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.Retry;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.Listeners;
import org.multiverse.stms.gamma.transactionalobjects.AbstractGammaRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaRefTranlocal;

public final class MonoGammaTransaction extends GammaTransaction {

    public final GammaRefTranlocal tranlocal = new GammaRefTranlocal();

    public MonoGammaTransaction(GammaStm stm) {
        this(new GammaTransactionConfiguration(stm));
    }

    public MonoGammaTransaction(GammaTransactionConfiguration config) {
        super(config, POOL_TRANSACTIONTYPE_MONO);
    }

    @Override
    public GammaRefTranlocal locate(AbstractGammaRef o) {
        if (status != TX_ACTIVE) {
            throw abortLocateOnBadStatus(o);
        }

        if (o == null) {
            throw abortLocateOnNullArgument();
        }

        return getRefTranlocal(o);
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

        final AbstractGammaRef owner = tranlocal.owner;

        if (owner != null) {
            if (status == TX_ACTIVE) {
                if (!owner.prepare(this, tranlocal)) {
                    throw abortOnReadWriteConflict();
                }
            }
            Listeners listeners = owner.safe(tranlocal, pool);
            if (listeners != null) {
                listeners.openAll(pool);
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
        AbstractGammaRef owner = tranlocal.owner;
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

        final AbstractGammaRef owner = tranlocal.owner;
        if (owner != null) {
            if (!owner.prepare(this, tranlocal)) {
                throw abortOnReadWriteConflict();
            }
        }

        status = TX_PREPARED;
    }

    @Override
    public GammaRefTranlocal getRefTranlocal(AbstractGammaRef ref) {
        //noinspection ObjectEquality
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

        if (tranlocal == null) {
            throw abortRetryOnNoRetryPossible();
        }

        final AbstractGammaRef owner = tranlocal.owner;
        if (owner == null) {
            throw abortRetryOnNoRetryPossible();
        }

        listener.reset();
        final long listenerEra = listener.getEra();

        boolean atLeastOneRegistration = false;
        switch (tranlocal.owner.registerChangeListener(listener, tranlocal, pool, listenerEra)) {
            case REGISTRATION_DONE:
                atLeastOneRegistration = true;
                break;
            case REGISTRATION_NOT_NEEDED:
                atLeastOneRegistration = true;
                break;
            case REGISTRATION_NONE:
                break;
            default:
                throw new IllegalStateException();
        }

        owner.releaseAfterFailure(tranlocal, pool);

        status = TX_ABORTED;

        if (!atLeastOneRegistration) {
            throw abortRetryOnNoRetryPossible();
        }

        throw Retry.INSTANCE;
    }

    @Override
    public boolean softReset() {
        if (attempt >= config.getMaxRetries()) {
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
        attempt = 1;
        abortOnly = false;
    }

    public boolean isReadConsistent(GammaRefTranlocal justAdded) {
        return true;
    }
}
