package org.multiverse.stms.alpha.manualinstrumentation;

import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTranlocalSnapshot;
import org.multiverse.stms.alpha.AlphaTransactionalObject;
import org.multiverse.stms.alpha.mixins.DefaultTxObjectMixin;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.templates.TransactionTemplate;

import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class LongRef extends DefaultTxObjectMixin {

    public LongRef(final long value) {
        new TransactionTemplate() {
            @Override
            public Object execute(Transaction t) {
                LongRefTranlocal tranlocal = (LongRefTranlocal) ((AlphaTransaction) t).openForWrite(LongRef.this);
                tranlocal.value = value;
                return null;
            }
        }.execute();
    }

    @TransactionalMethod
    public void await(long expectedValue) {
        AlphaTransaction t = (AlphaTransaction) getThreadLocalTransaction();
        LongRefTranlocal tranlocal = (LongRefTranlocal) t.openForWrite(LongRef.this);
        await(tranlocal, expectedValue);
    }

    @TransactionalMethod
    public void set(long value) {
        AlphaTransaction t = (AlphaTransaction) getThreadLocalTransaction();
        LongRefTranlocal tranlocal = (LongRefTranlocal) t.openForWrite(LongRef.this);
        set(tranlocal, value);
    }

    @TransactionalMethod(readonly = true)
    public long get() {
        AlphaTransaction t = (AlphaTransaction) getThreadLocalTransaction();
        LongRefTranlocal tranlocal = (LongRefTranlocal) t.openForWrite(LongRef.this);
        return get(tranlocal);
    }

    @TransactionalMethod
    public void inc() {
        AlphaTransaction t = (AlphaTransaction) getThreadLocalTransaction();
        LongRefTranlocal tranlocal = (LongRefTranlocal) t.openForWrite(LongRef.this);
        inc(tranlocal);
    }

    @TransactionalMethod
    public void dec() {
        AlphaTransaction t = (AlphaTransaction) getThreadLocalTransaction();
        LongRefTranlocal tranlocal = (LongRefTranlocal) t.openForWrite(LongRef.this);
        dec(tranlocal);
    }

    @Override
    public LongRefTranlocal ___openUnconstructed() {
        return new LongRefTranlocal(this);
    }

    public void set(LongRefTranlocal tranlocal, long newValue) {
        if (tranlocal.isCommitted()) {
            throw new ReadonlyException();
        }
        tranlocal.value = newValue;
    }

    public long get(LongRefTranlocal tranlocal) {
        return tranlocal.value;
    }

    public void inc(LongRefTranlocal tranlocal) {
        if (tranlocal.isCommitted()) {
            throw new ReadonlyException();
        }
        tranlocal.value++;
    }

    public void dec(LongRefTranlocal tranlocal) {
        if (tranlocal.isCommitted()) {
            throw new ReadonlyException();
        }
        tranlocal.value--;
    }

    public void await(LongRefTranlocal tranlocal, long expectedValue) {
        if (tranlocal.value != expectedValue) {
            retry();
        }
    }
}

class LongRefTranlocal extends AlphaTranlocal {

    private final LongRef ___txObject;
    LongRefTranlocal ___origin;
    public long value;

    public LongRefTranlocal(LongRefTranlocal origin) {
        this.___origin = origin;
        this.___txObject = origin.___txObject;
        this.value = origin.value;
    }

    public LongRefTranlocal(LongRef txObject) {
        this.___txObject = txObject;
    }

    @Override
    public AlphaTranlocal openForWrite() {
        return new LongRefTranlocal(this);
    }

    @Override
    public AlphaTranlocal getOrigin() {
        return ___origin;
    }

    @Override
    public AlphaTransactionalObject getTransactionalObject() {
        return ___txObject;
    }

    @Override
    public void prepareForCommit(long writeVersion) {
        this.___writeVersion = writeVersion;
        this.___origin = null;
    }

    @Override
    public boolean isDirty() {
        if (isCommitted()) {
            return false;
        }

        if (___origin == null) {
            return true;
        }

        if (___origin.value != value) {
            return true;
        }

        return false;
    }

    @Override
    public AlphaTranlocalSnapshot takeSnapshot() {
        return new LongRefTranlocalSnapshot(this);
    }
}

class LongRefTranlocalSnapshot extends AlphaTranlocalSnapshot {

    final LongRefTranlocal ___tranlocal;
    final long value;

    LongRefTranlocalSnapshot(LongRefTranlocal tranlocal) {
        this.___tranlocal = tranlocal;
        this.value = tranlocal.value;
    }

    @Override
    public AlphaTranlocal getTranlocal() {
        return ___tranlocal;
    }

    @Override
    public void restore() {
        ___tranlocal.value = value;
    }
}