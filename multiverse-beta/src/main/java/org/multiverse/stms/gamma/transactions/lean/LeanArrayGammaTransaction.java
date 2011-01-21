package org.multiverse.stms.gamma.transactions.lean;

import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.ExplicitAbortException;
import org.multiverse.api.exceptions.Retry;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.Listeners;
import org.multiverse.stms.gamma.transactionalobjects.AbstractGammaRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaRefTranlocal;
import org.multiverse.stms.gamma.transactions.GammaTransaction;
import org.multiverse.stms.gamma.transactions.GammaTransactionConfiguration;

public class LeanArrayGammaTransaction extends GammaTransaction {

    public final GammaRefTranlocal[] tranlocals;
    public int size = 0;
    public boolean hasReads = false;
    public final Listeners[] listenersArray;

    public LeanArrayGammaTransaction(final GammaStm stm) {
        this(new GammaTransactionConfiguration(stm));
    }

    @SuppressWarnings({"ObjectAllocationInLoop"})
    public LeanArrayGammaTransaction(final GammaTransactionConfiguration config) {
        super(config, POOL_TRANSACTIONTYPE_ARRAY);

        listenersArray = new Listeners[config.arrayTransactionSize];
        tranlocals = new GammaRefTranlocal[config.arrayTransactionSize];

        for (int k = 0; k < config.arrayTransactionSize; k++) {
            tranlocals[k] = new GammaRefTranlocal();
        }
    }

    public final boolean isReadConsistent(GammaRefTranlocal justAdded) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void commit() {
        int s = status;

        if (s == TX_COMMITTED) {
            return;
        }

        if (s != TX_ACTIVE && s != TX_PREPARED) {
            throw abortCommitOnBadStatus();
        }

        if (hasWrites) {
            if (s == TX_ACTIVE) {
                if (!prepareChainForCommit()) {
                    throw abortOnReadWriteConflict();
                }
            }

            int listenersIndex = 0;

            final GammaRefTranlocal[] array = tranlocals;
            for (int k = 0; k < array.length; k++) {
                final GammaRefTranlocal tranlocal = array[k];
                final AbstractGammaRef owner = tranlocal.owner;

                if (owner == null) {
                    break;
                }

                final Listeners listeners = owner.leanSafe(tranlocal);
                if (listeners != null) {
                    listenersArray[listenersIndex] = listeners;
                    listenersIndex++;
                }
            }

            if (listenersArray != null) {
                Listeners.openAll(listenersArray, pool);
            }
        } else {
            releaseReadonlyChain();
        }

        status = TX_COMMITTED;
    }

    @SuppressWarnings({"BooleanMethodIsAlwaysInverted"})
    private boolean prepareChainForCommit() {
        final GammaRefTranlocal[] array = tranlocals;
        for (int k = 0; k < array.length; k++) {
            GammaRefTranlocal tranlocal = array[k];
            final AbstractGammaRef owner = tranlocal.owner;

            if (owner == null) {
                return true;
            }

            if (tranlocal.mode == TRANLOCAL_READ) {
                continue;
            }

            if (!owner.prepare(this, tranlocal)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public final void abort() {
        if (status == TX_ABORTED) {
            return;
        }

        if (status == TX_COMMITTED) {
            throw new DeadTransactionException();
        }

        releaseChainForAbort();
        status = TX_ABORTED;
    }

    private void releaseChainForAbort() {
        final GammaRefTranlocal[] array = tranlocals;

        for (int k = 0; k < array.length; k++) {
            GammaRefTranlocal tranlocal = array[k];
            final AbstractGammaRef owner = tranlocal.owner;

            if (owner == null) {
                return;
            }

            if (tranlocal.isWrite()) {
                if (tranlocal.getLockMode() == LOCKMODE_EXCLUSIVE) {
                    if (tranlocal.hasDepartObligation()) {
                        tranlocal.setDepartObligation(false);
                        owner.departAfterFailureAndUnlock();
                    } else {
                        owner.unlockByUnregistered();
                    }
                }
            }

            tranlocal.owner = null;
            tranlocal.ref_oldValue = null;
            tranlocal.ref_value = null;
        }
    }

    private void releaseReadonlyChain() {
        final GammaRefTranlocal[] array = tranlocals;
        for (int k = 0; k < array.length; k++) {
            final GammaRefTranlocal tranlocal = array[k];
            final AbstractGammaRef owner = tranlocal.owner;

            if (owner == null) {
                return;
            }

            tranlocal.owner = null;
            tranlocal.ref_oldValue = null;
            tranlocal.ref_value = null;
        }
    }

    @Override
    public final GammaRefTranlocal getRefTranlocal(final AbstractGammaRef ref) {
        final GammaRefTranlocal[] array = tranlocals;

        for (int k = 0; k < array.length; k++) {
            final GammaRefTranlocal tranlocal = array[k];
            final AbstractGammaRef owner = tranlocal.owner;

            if (owner == null) {
                return null;
            }

            //noinspection ObjectEquality
            if (owner == ref) {
                return tranlocal;
            }
        }
        return null;
    }

    @Override
    public final void retry() {
        if (status != TX_ACTIVE) {
            throw abortRetryOnBadStatus();
        }

        if (!config.isBlockingAllowed()) {
            throw abortRetryOnNoBlockingAllowed();
        }

        if (size == 0) {
            throw abortRetryOnNoRetryPossible();
        }

        listener.reset();
        final long listenerEra = listener.getEra();

        boolean furtherRegistrationNeeded = true;
        boolean atLeastOneRegistration = false;

        final GammaRefTranlocal[] array = tranlocals;
        for (int k = 0; k < array.length; k++) {
            final GammaRefTranlocal tranlocal = array[k];
            final AbstractGammaRef owner = tranlocal.owner;

            if (owner == null) {
                break;
            }

            if (furtherRegistrationNeeded) {
                switch (owner.registerChangeListener(listener, tranlocal, pool, listenerEra)) {
                    case REGISTRATION_DONE:
                        atLeastOneRegistration = true;
                        break;
                    case REGISTRATION_NOT_NEEDED:
                        furtherRegistrationNeeded = false;
                        atLeastOneRegistration = true;
                        break;
                    case REGISTRATION_NONE:
                        break;
                    default:
                        throw new IllegalStateException();
                }
            }

            owner.releaseAfterFailure(tranlocal, pool);
        }

        status = TX_ABORTED;

        if (!atLeastOneRegistration) {
            throw abortRetryOnNoRetryPossible();
        }

        if (config.controlFlowErrorsReused) {
            throw Retry.INSTANCE;
        } else {
            throw new Retry();
        }
    }

    @Override
    public final void prepare() {
        if (status == TX_PREPARED) {
            return;
        }

        if (status != TX_ACTIVE) {
            throw abortPrepareOnBadStatus();
        }

        if (abortOnly) {
            abort();
            throw new ExplicitAbortException();
        }

        if (!prepareChainForCommit()) {
            throw abortOnReadWriteConflict();
        }

        status = TX_PREPARED;
    }

    @Override
    public final GammaRefTranlocal locate(AbstractGammaRef o) {
        if (status != TX_ACTIVE) {
            throw abortLocateOnBadStatus(o);
        }

        if (o == null) {
            throw abortLocateOnNullArgument();
        }

        return getRefTranlocal(o);
    }

    public final void hardReset() {
        status = TX_ACTIVE;
        hasWrites = false;
        size = 0;
        remainingTimeoutNs = config.timeoutNs;
        attempt = 1;
        hasReads = false;
        abortOnly = false;
    }

    @Override
    public final boolean softReset() {
        if (attempt >= config.getMaxRetries()) {
            return false;
        }

        status = TX_ACTIVE;
        hasWrites = false;
        size = 0;
        hasReads = false;
        abortOnly = false;
        attempt++;
        return true;
    }
}
