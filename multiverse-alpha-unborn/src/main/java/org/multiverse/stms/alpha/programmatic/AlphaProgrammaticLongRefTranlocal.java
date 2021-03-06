package org.multiverse.stms.alpha.programmatic;

import org.multiverse.api.*;
import org.multiverse.api.exceptions.UncommittedReadConflict;
import org.multiverse.api.latches.Latch;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTranlocalSnapshot;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.utils.TodoException;

/**
 * The {@link AlphaTranlocal} for the {@link AlphaProgrammaticLongRef}.
 * <p/>
 * It also implement the transaction interface. This is an optimization needed for the atomic methods,
 * to prevent creating an expensive transaction object.
 *
 * @author Peter Veentjer
 */
public final class AlphaProgrammaticLongRefTranlocal extends AlphaTranlocal implements Transaction {

    public long value;
    public long commutingIncrements;

    public AlphaProgrammaticLongRefTranlocal(AlphaProgrammaticLongRef transactionalObject, boolean commuting) {
        this.___transactionalObject = transactionalObject;
        this.___writeVersion = commuting ? OPENED_FOR_COMMUTE : OPENED_FOR_WRITE;
    }

    public AlphaProgrammaticLongRefTranlocal(AlphaProgrammaticLongRefTranlocal origin) {
        this.___origin = origin;
        this.___transactionalObject = origin.___transactionalObject;
        this.value = origin.value;
    }

    @Override
    public void prematureFixation(AlphaTransaction tx, AlphaTranlocal origin) {
        if (!isCommuting()) {
            return;
        }

        this.___origin = origin;
        this.___writeVersion = OPENED_FOR_WRITE;
        this.value = ((AlphaProgrammaticLongRefTranlocal) origin).value;
        this.value += commutingIncrements;
        this.commutingIncrements = 0;
    }

    @Override
    public void lateFixation(AlphaTransaction tx) {
        if (!isCommuting()) {
            return;
        }

        AlphaProgrammaticLongRefTranlocal origin = (AlphaProgrammaticLongRefTranlocal) ___transactionalObject.___load();
        if (origin == null) {
            throw new UncommittedReadConflict();
        }

        this.___origin = origin;
        this.___writeVersion = OPENED_FOR_WRITE_AND_DIRTY;
        this.value = origin.value;
        this.value += commutingIncrements;
        this.commutingIncrements = 0;
    }

    @Override
    public AlphaTranlocalSnapshot takeSnapshot() {
        throw new TodoException();
    }

    @Override
    public AlphaTranlocal openForWrite() {
        return new AlphaProgrammaticLongRefTranlocal(this);
    }

    @Override
    public boolean isDirty() {
        if (isCommitted()) {
            return false;
        }

        if (___origin == null) {
            return true;
        }

        if (isCommuting()) {
            return commutingIncrements != 0;
        }

        AlphaProgrammaticLongRefTranlocal org = (AlphaProgrammaticLongRefTranlocal) ___origin;
        return org.value != value;
    }

    @Override
    public void abort() {
        //ignore
    }

    @Override
    public TransactionConfiguration getConfiguration() {
        //ignore
        return null;
    }

    @Override
    public long getReadVersion() {
        //ignore
        return 0;
    }

    @Override
    public TransactionStatus getStatus() {
        //ignore
        return null;
    }

    @Override
    public void commit() {
        //ignore
    }

    @Override
    public void prepare() {
        //ignore
    }

    @Override
    public void reset() {
        //ignore
    }

    @Override
    public void registerRetryLatch(Latch latch) {
        //ignore
    }

    @Override
    public void registerLifecycleListener(TransactionLifecycleListener listener) {
        //ignore
    }

    @Override
    public long getRemainingTimeoutNs() {
        return Long.MAX_VALUE;
    }

    @Override
    public void setRemainingTimeoutNs(long timeoutNs) {
        //ignore
    }

    @Override
    public void start() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stm getStm() {
        throw new UnsupportedOperationException();
    }

    @Override
    public TransactionFactory getTransactionFactory() {
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
