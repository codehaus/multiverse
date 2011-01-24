package org.multiverse.stms.gamma.transactions.fat;

import org.multiverse.api.exceptions.AbortOnlyException;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.Retry;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.Listeners;
import org.multiverse.stms.gamma.transactionalobjects.AbstractGammaRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaRefTranlocal;
import org.multiverse.stms.gamma.transactions.GammaTransaction;
import org.multiverse.stms.gamma.transactions.GammaTransactionConfiguration;

public final class FatVariableLengthGammaTransaction extends GammaTransaction {

    public GammaRefTranlocal[] array;
    public int size = 0;
    public boolean hasReads = false;

    public FatVariableLengthGammaTransaction(GammaStm stm) {
        this(new GammaTransactionConfiguration(stm));
    }

    public FatVariableLengthGammaTransaction(GammaTransactionConfiguration config) {
        super(config, TRANSACTIONTYPE_FAT_VARIABLE_LENGTH);
        this.array = new GammaRefTranlocal[config.minimalArrayTreeSize];
    }

    public final float getUsage() {
        return (size * 1.0f) / array.length;
    }

    public final int size() {
        return size;
    }

    public final int indexOf(final AbstractGammaRef ref, final int hash) {
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

    public final void attach(final GammaRefTranlocal tranlocal, final int hash) {
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
    public final void commit() {
        if (status == TX_COMMITTED) {
            return;
        }

        if (status != TX_ACTIVE && status != TX_PREPARED) {
            throw abortCommitOnBadStatus();
        }

        if (abortOnly) {
            abort();
            throw new AbortOnlyException();
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
    public final void prepare() {
        if (status == TX_PREPARED) {
            return;
        }

        if (status != TX_ACTIVE) {
            throw abortPrepareOnBadStatus();
        }

        if (abortOnly) {
            abort();
            throw new AbortOnlyException();
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
    public final void abort() {
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
    public final GammaRefTranlocal locate(AbstractGammaRef o) {
        if (status != TX_ACTIVE) {
            throw abortLocateOnBadStatus(o);
        }

        if (o == null) {
            throw abortLocateOnNullArgument();
        }

        return getRefTranlocal(o);
    }

    @Override
    public final GammaRefTranlocal getRefTranlocal(AbstractGammaRef ref) {
        int indexOf = indexOf(ref, ref.identityHashCode());
        return indexOf == -1 ? null : array[indexOf];
    }

    @Override
    public final void retry() {
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

        if (config.controlFlowErrorsReused) {
            throw Retry.INSTANCE;
        } else {
            throw new Retry();
        }
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

    @Override
    public final void hardReset() {
        status = TX_ACTIVE;
        hasWrites = false;
        remainingTimeoutNs = config.timeoutNs;
        poorMansConflictScan = !config.speculativeConfiguration.get().isRichMansConflictScanRequired;
        attempt = 1;
        size = 0;
        hasReads = false;
        abortOnly = false;
    }

    public final boolean isReadConsistent(GammaRefTranlocal justAdded) {
        if (!hasReads) {
            return true;
        }

        if (config.readLockModeAsInt > LOCKMODE_NONE) {
            return true;
        }

        if (poorMansConflictScan) {
            if (size > config.maximumPoorMansConflictScanLength) {
                throw abortOnTransactionTooLargeForPoorMansConflictScan();
            }
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
}
