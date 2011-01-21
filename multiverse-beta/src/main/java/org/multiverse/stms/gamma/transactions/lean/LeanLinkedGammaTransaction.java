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

public final class LeanLinkedGammaTransaction extends GammaTransaction {

    public GammaRefTranlocal head;
    public int size = 0;
    public boolean hasReads = false;
    public final Listeners[] listenersArray;

    public LeanLinkedGammaTransaction(final GammaStm stm) {
        this(new GammaTransactionConfiguration(stm));
    }

    @SuppressWarnings({"ObjectAllocationInLoop"})
    public LeanLinkedGammaTransaction(final GammaTransactionConfiguration config) {
        super(config, POOL_TRANSACTIONTYPE_ARRAY);

        listenersArray = new Listeners[config.arrayTransactionSize];

        GammaRefTranlocal h = null;
        for (int k = 0; k < config.arrayTransactionSize; k++) {
            GammaRefTranlocal newNode = new GammaRefTranlocal();
            if (h != null) {
                h.previous = newNode;
                newNode.next = h;
            }

            h = newNode;
        }
        head = h;
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
            GammaRefTranlocal node = head;
            do {
                final AbstractGammaRef owner = node.owner;

                if (owner == null) {
                    break;
                }

                final Listeners listeners = owner.leanSafe(node);
                if (listeners != null) {
                    listenersArray[listenersIndex] = listeners;
                    listenersIndex++;
                }
                node = node.next;
            } while (node != null);

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
        GammaRefTranlocal node = head;

        do {
            final AbstractGammaRef owner = node.owner;

            if (owner == null) {
                return true;
            }

            if (node.mode == TRANLOCAL_READ) {
                continue;
            }

            if (!owner.prepare(this, node)) {
                return false;
            }

            node = node.next;
        } while (node != null);

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
        GammaRefTranlocal node = head;
        do {
            final AbstractGammaRef owner = node.owner;

            if (owner == null) {
                return;
            }

            if (node.isWrite()) {
                if (node.getLockMode() == LOCKMODE_EXCLUSIVE) {
                    if (node.hasDepartObligation()) {
                        node.setDepartObligation(false);
                        owner.departAfterFailureAndUnlock();
                    } else {
                        owner.unlockByUnregistered();
                    }
                }
            }

            node.owner = null;
            node.ref_oldValue = null;
            node.ref_value = null;
            node = node.next;
        } while (node != null);
    }

    private void releaseReadonlyChain() {
        GammaRefTranlocal node = head;
        do {
            final AbstractGammaRef owner = node.owner;

            if (owner == null) {
                return;
            }

            node.owner = null;
            node.ref_oldValue = null;
            node.ref_value = null;
            node = node.next;
        } while (node != null);
    }

    @Override
    public final GammaRefTranlocal getRefTranlocal(final AbstractGammaRef ref) {
        GammaRefTranlocal node = head;
        do {
            //noinspection ObjectEquality
            if (node.owner == ref) {
                return node;
            }

            if (node.owner == null) {
                return null;
            }

            node = node.next;
        } while (node != null);
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

        GammaRefTranlocal tranlocal = head;
        do {
            final AbstractGammaRef owner = tranlocal.owner;

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
            tranlocal = tranlocal.next;
        } while (tranlocal != null && tranlocal.owner != null);

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

    public final void shiftInFront(GammaRefTranlocal newHead) {
        //noinspection ObjectEquality
        if (newHead == head) {
            return;
        }

        head.previous = newHead;
        if (newHead.next != null) {
            newHead.next.previous = newHead.previous;
        }
        newHead.previous.next = newHead.next;
        newHead.next = head;
        newHead.previous = null;
        head = newHead;
    }
}
