package org.multiverse.stms.alpha.programmatic;

import org.multiverse.api.*;
import org.multiverse.api.latches.Latch;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTranlocalSnapshot;
import org.multiverse.utils.TodoException;

/**
 * The AlphaTranlocal for the AlphaProgrammaticRef. It is responsible for storing the state of the AlphaProgrammaticRef.
 * <p/>
 * The AlpaRefTranlocal also implements the Transaction interface because it can be
 * used as a lockOwner. This is done as a performance optimization.
 *
 * @param <E>
 */
public final class AlphaProgrammaticRefTranlocal<E> extends AlphaTranlocal implements Transaction {

    public E value;

    public AlphaProgrammaticRefTranlocal(AlphaProgrammaticRefTranlocal<E> origin) {
        this.___origin = origin;
        this.___transactionalObject = origin.___transactionalObject;
        this.value = origin.value;
    }

    public AlphaProgrammaticRefTranlocal(AlphaProgrammaticRef<E> owner) {
        this(owner, null);
    }

    public AlphaProgrammaticRefTranlocal(AlphaProgrammaticRef<E> owner, E value) {
        this.___transactionalObject = owner;
        this.value = value;
    }

    @Override
    public AlphaTranlocal openForWrite() {
        return new AlphaProgrammaticRefTranlocal(this);
    }

    @Override
    public AlphaTranlocalSnapshot takeSnapshot() {
        throw new TodoException();
    }

    @Override
    public Stm getStm() {
        throw new TodoException();
    }

    @Override
    public TransactionFactory getTransactionFactory() {
        throw new TodoException();
    }

    @Override
    public boolean isDirty() {
        if (isCommitted()) {
            return false;
        }

        if (___origin == null) {
            return true;
        }

        AlphaProgrammaticRefTranlocal origin = (AlphaProgrammaticRefTranlocal) ___origin;
        if (origin.value != this.value) {
            return true;
        }

        return false;
    }

    @Override
    public void abort() {
        throw new UnsupportedOperationException();
    }

    @Override
    public TransactionConfiguration getConfiguration() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getReadVersion() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void start() {
        throw new UnsupportedOperationException();
    }

    @Override
    public TransactionStatus getStatus() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void commit() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void prepare() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void registerRetryLatch(Latch latch) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void registerLifecycleListener(TransactionLifecycleListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getRemainingTimeoutNs() {
        return Long.MAX_VALUE;
    }

    @Override
    public void setRemainingTimeoutNs(long timeoutNs) {
        throw new UnsupportedOperationException();
    }

    private int attempt;

    @Override
    public int getAttempt() {
        return attempt;
    }

    @Override
    public void setAttempt(int attempt) {
        this.attempt = attempt;
    }
}
