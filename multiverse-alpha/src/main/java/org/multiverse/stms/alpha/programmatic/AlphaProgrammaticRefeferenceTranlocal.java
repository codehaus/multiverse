package org.multiverse.stms.alpha.programmatic;

import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionConfiguration;
import org.multiverse.api.TransactionStatus;
import org.multiverse.api.latches.Latch;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTranlocalSnapshot;
import org.multiverse.utils.TodoException;

/**
 * The AlphaTranlocal for the AlphaProgrammaticReference. It is responsible for storing the state of the AlphaProgrammaticReference.
 * <p/>
 * The AlpaRefTranlocal also implements the Transaction interface because it can be
 * used as a lockOwner. This is done as a performance optimization.
 *
 * @param <E>
 */
public class AlphaProgrammaticRefeferenceTranlocal<E> extends AlphaTranlocal implements Transaction {

    E value;

    public AlphaProgrammaticRefeferenceTranlocal(AlphaProgrammaticRefeferenceTranlocal<E> origin) {
        this.___origin = origin;
        this.___transactionalObject = origin.___transactionalObject;
        this.value = origin.value;
    }

    public AlphaProgrammaticRefeferenceTranlocal(AlphaProgrammaticReference<E> owner) {
        this(owner, null);
    }

    public AlphaProgrammaticRefeferenceTranlocal(AlphaProgrammaticReference<E> owner, E value) {
        this.___transactionalObject = owner;
        this.value = value;
    }

    @Override
    public AlphaTranlocal openForWrite() {
        return new AlphaProgrammaticRefeferenceTranlocal(this);
    }

    @Override
    public AlphaTranlocalSnapshot takeSnapshot() {
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

        AlphaProgrammaticRefeferenceTranlocal origin = (AlphaProgrammaticRefeferenceTranlocal) ___origin;
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
    public void restart() {
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
}
