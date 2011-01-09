package org.multiverse.stms.gamma.transactions;

import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaObject;
import org.multiverse.stms.gamma.transactionalobjects.GammaTranlocal;

public final class ArrayGammaTransaction extends GammaTransaction {

    public GammaTranlocal head;
    public int size = 0;

    public ArrayGammaTransaction(GammaStm stm) {
        this(new GammaTransactionConfiguration(stm));
    }

    public ArrayGammaTransaction(GammaTransactionConfiguration config) {
        super(config);

        GammaTranlocal h = null;
        for (int k = 0; k < config.arraySize; k++) {
            GammaTranlocal newNode = new GammaTranlocal();
            if (h != null) {
                h.previous = newNode;
                newNode.next = h;
            }

            h = newNode;
        }
        head = h;
    }

    @Override
    public GammaTranlocal openForRead(GammaLongRef o, int lockMode) {
        return o.openForRead(this, lockMode);
    }

    @Override
    public GammaTranlocal openForWrite(GammaLongRef o, int lockMode) {
        return o.openForWrite(this, lockMode);
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
                    if (!prepareChainForCommit()) {
                        throw abortOnReadWriteConflict();
                    }
                }

                commitChain();
            } else {
                releaseChain(true);
            }
        }

        status = TX_COMMITTED;
    }

    private void commitChain() {
        GammaTranlocal node = head;
        do {
            GammaObject owner = node.owner;
            if (owner == null) {
                return;
            }

            if (node.isDirty()) {
                GammaLongRef ref = (GammaLongRef) owner;
                ref.value = node.long_value;
                ref.version++;
                ref.releaseAfterUpdate(node, pool);
            } else {
                owner.releaseAfterReading(node, pool);
            }

            node = node.next;
        } while (node != null);
    }

    private boolean prepareChainForCommit() {
        GammaTranlocal node = head;

        do {
            if (node.owner == null) {
                return true;
            }

            if (!node.prepare(config)) {
                return false;
            }

            node = node.next;
        } while (node != null);

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

        releaseChain(false);
        status = TX_ABORTED;
    }

    private void releaseChain(boolean success) {
        GammaTranlocal node = head;
        while (node != null) {
            GammaObject owner = node.owner;

            if(owner == null){
                return;
            }

            if(success){
                owner.releaseAfterReading(node, pool);
            }else{
                owner.releaseAfterFailure(node, pool);
            }

            node = node.next;
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

        if (!prepareChainForCommit()) {
            throw abortOnReadWriteConflict();
        }

        status = TX_PREPARED;
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

    public void hardReset() {
        status = TX_ACTIVE;
        hasWrites = false;
        size = 0;
        remainingTimeoutNs = config.timeoutNs;
        attempt = 0;
    }

    public void shiftInFront(GammaTranlocal newHead) {
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

    @Override
    public GammaTranlocal get(GammaObject ref) {
        GammaTranlocal node = head;
        while (node != null) {
            if (node.owner == ref) {
                return node;
            }

            if (node.owner == null) {
                return null;
            }

            node = node.next;
        }
        return null;
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
