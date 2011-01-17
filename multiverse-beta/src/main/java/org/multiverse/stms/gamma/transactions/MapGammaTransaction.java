package org.multiverse.stms.gamma.transactions;

import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.Retry;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.Listeners;
import org.multiverse.stms.gamma.transactionalobjects.AbstractGammaRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaRefTranlocal;

public final class MapGammaTransaction extends GammaTransaction {

    public GammaRefTranlocal[] array;
    public int size = 0;
    public boolean needsConsistency = false;

    public MapGammaTransaction(GammaStm stm) {
        this(new GammaTransactionConfiguration(stm));
    }

    public MapGammaTransaction(GammaTransactionConfiguration config) {
        super(config, POOL_TRANSACTIONTYPE_MAP);
        this.array = new GammaRefTranlocal[config.minimalArrayTreeSize];
    }

    public float getUsage() {
        return (size * 1.0f) / array.length;
    }

    public int size() {
        return size;
    }

    public int indexOf(final AbstractGammaRef ref, final int hash) {
        int jump = 0;
        boolean goLeft = true;

        do {
            final int offset = goLeft ? -jump : jump;
            final int index = (hash + offset) % array.length;

            final GammaRefTranlocal current = array[index];
            if (current == null || current.owner == null) {
                return -1;
            }

            //noinspection ObjectEquality
            if (current.owner == ref) {
                return index;
            }

            final int currentHash = current.owner.identityHashCode();
            goLeft = currentHash > hash;
            jump = jump == 0 ? 1 : jump * 2;
        } while (jump < array.length);

        return -1;
    }

    public void attach(final GammaRefTranlocal tranlocal, final int hash) {
        int jump = 0;
        boolean goLeft = true;

        do {
            final int offset = goLeft ? -jump : jump;
            final int index = (hash + offset) % array.length;

            GammaRefTranlocal current = array[index];
            if (current == null) {
                array[index] = tranlocal;
                return;
            }

            final int currentHash = current.owner.identityHashCode();
            goLeft = currentHash > hash;
            jump = jump == 0 ? 1 : jump * 2;
        } while (jump < array.length);

        expand();
        attach(tranlocal, hash);
    }

    private void expand() {
        GammaRefTranlocal[] oldArray = array;
        int newSize = oldArray.length * 2;
        array = pool.takeTranlocalArray(newSize);

        for (int k = 0; k < oldArray.length; k++) {
            final GammaRefTranlocal tranlocal = oldArray[k];

            if (tranlocal == null) {
                continue;
            }

            oldArray[k] = null;
            attach(tranlocal, tranlocal.owner.identityHashCode());
        }

        pool.putTranlocalArray(oldArray);
    }

    @Override
    public void commit() {
        if (status == TX_COMMITTED) {
            return;
        }

        if (status != TX_ACTIVE && status != TX_PREPARED) {
            throw abortCommitOnBadStatus();
        }

        if (size > 0) {
            if (hasWrites) {
                if (status == TX_ACTIVE) {
                    if (!doPrepare()) {
                        throw abortOnReadWriteConflict();
                    }
                }

                Listeners[] listenersArray = commitArray();

                if (listenersArray != null) {
                    Listeners.openAll(listenersArray, pool);
                    pool.putListenersArray(listenersArray);
                }
            } else {
                releaseArray(true);
            }
        }

        status = TX_COMMITTED;
    }

    private Listeners[] commitArray() {
        Listeners[] listenersArray = null;

        int listenersIndex = 0;
        int itemCount = 0;
        for (int k = 0; k < array.length; k++) {
            final GammaRefTranlocal tranlocal = array[k];

            if (tranlocal == null) {
                continue;
            }

            array[k] = null;

            final AbstractGammaRef owner = tranlocal.owner;
            final Listeners listeners = owner.safe(tranlocal, pool);
            if (listeners != null) {
                if (listenersArray == null) {
                    listenersArray = pool.takeListenersArray(size - itemCount);
                }

                listenersArray[listenersIndex] = listeners;
                listenersIndex++;
            }
            pool.put(tranlocal);
            itemCount++;
        }

        return listenersArray;
    }

    private void releaseArray(boolean success) {
        for (int k = 0; k < array.length; k++) {
            final GammaRefTranlocal tranlocal = array[k];

            if (tranlocal != null) {
                array[k] = null;
                if (success) {
                    tranlocal.owner.releaseAfterReading(tranlocal, pool);
                } else {
                    tranlocal.owner.releaseAfterFailure(tranlocal, pool);
                }
                pool.put(tranlocal);
            }
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

        if (hasWrites) {
            if (!doPrepare()) {
                throw abortOnReadWriteConflict();
            }
        }

        status = TX_PREPARED;
    }

    @SuppressWarnings({"BooleanMethodIsAlwaysInverted"})
    private boolean doPrepare() {
        for (int k = 0; k < array.length; k++) {
            final GammaRefTranlocal tranlocal = array[k];

            if (tranlocal == null) {
                continue;
            }

            if (!tranlocal.owner.prepare(this, tranlocal)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void abort() {
        if (status == TX_ABORTED) {
            return;
        }

        if (status == TX_COMMITTED) {
            throw new DeadTransactionException();
        }

        if (size > 0) {
            releaseArray(false);
        }

        status = TX_ABORTED;
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
    public GammaRefTranlocal getRefTranlocal(AbstractGammaRef ref) {
        int indexOf = indexOf(ref, ref.identityHashCode());
        return indexOf == -1 ? null : array[indexOf];
    }

    @Override
    public void retry() {
        if (status != TX_ACTIVE && status != TX_PREPARED) {
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

        for (int k = 0; k < array.length; k++) {
            final GammaRefTranlocal tranlocal = array[k];
            if (tranlocal == null) {
                continue;
            }

            array[k] = null;

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
            pool.put(tranlocal);
        }

        status = TX_ABORTED;

        if (!atLeastOneRegistration) {
            throw abortRetryOnNoRetryPossible();
        }

        throw Retry.INSTANCE;
    }

    public boolean isReadConsistent(GammaRefTranlocal justAdded) {
        if (!needsConsistency) {
            return true;
        }

        if (config.readLockModeAsInt > LOCKMODE_NONE) {
            return true;
        }

        if (arriveEnabled) {

        }

        //doing a full conflict scan
        for (int k = 0; k < array.length; k++) {
            final GammaRefTranlocal tranlocal = array[k];
            //noinspection ObjectEquality
            if (tranlocal == null || tranlocal == justAdded) {
                continue;
            }

            if (tranlocal.owner.hasReadConflict(tranlocal)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean softReset() {
        if (attempt >= config.getMaxRetries()) {
            return false;
        }

        status = TX_ACTIVE;
        hasWrites = false;
        size = 0;
        needsConsistency = false;
        abortOnly = false;
        attempt++;
        return true;
    }

    @Override
    public void hardReset() {
        status = TX_ACTIVE;
        hasWrites = false;
        remainingTimeoutNs = config.timeoutNs;
        attempt = 0;
        size = 0;
        needsConsistency = false;
        abortOnly = false;
    }
}
