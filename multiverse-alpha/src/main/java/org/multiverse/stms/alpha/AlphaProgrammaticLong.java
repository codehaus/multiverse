package org.multiverse.stms.alpha;

import org.multiverse.api.ProgrammaticLong;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionStatus;
import org.multiverse.api.exceptions.UncommittedReadConflict;
import org.multiverse.stms.alpha.mixins.DefaultTxObjectMixin;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.templates.TransactionTemplate;
import org.multiverse.utils.TodoException;

import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticLong extends DefaultTxObjectMixin implements ProgrammaticLong {

    public AlphaProgrammaticLong(final long value) {
        new TransactionTemplate() {
            @Override
            public Object execute(Transaction tx) throws Exception {
                AlphaTransaction alphaTx = (AlphaTransaction) tx;
                AlphaProgrammaticLongTranlocal tranlocal = (AlphaProgrammaticLongTranlocal) alphaTx.openForConstruction(AlphaProgrammaticLong.this);
                tranlocal.value = value;
                return null;
            }
        }.execute();
    }

     public AlphaProgrammaticLong(Stm stm , final long value) {
        new TransactionTemplate(stm) {
            @Override
            public Object execute(Transaction tx) throws Exception {
                AlphaTransaction alphaTx = (AlphaTransaction) tx;
                AlphaProgrammaticLongTranlocal tranlocal = (AlphaProgrammaticLongTranlocal) alphaTx.openForConstruction(AlphaProgrammaticLong.this);
                tranlocal.value = value;
                return null;
            }
        }.execute();
    }

    public AlphaProgrammaticLong(AlphaTransaction tx, long value) {
        AlphaProgrammaticLongTranlocal tranlocal = (AlphaProgrammaticLongTranlocal) tx.openForConstruction(this);
        tranlocal.value = value;
    }

    @Override
    public long getAtomic() {
        AlphaProgrammaticLongTranlocal tranlocal = (AlphaProgrammaticLongTranlocal) ___load();
        if (tranlocal == null) {
            return 0;
        }

        return tranlocal.value;
    }

    @Override
    public long get(Transaction tx) {
        AlphaTransaction alphaTx = (AlphaTransaction) tx;
        AlphaProgrammaticLongTranlocal tranlocal = (AlphaProgrammaticLongTranlocal) alphaTx.openForRead(this);
        return tranlocal.value;
    }

    public long get() {
        Transaction tx = getThreadLocalTransaction();
        if (tx != null && tx.getStatus() == TransactionStatus.active) {
            return get(tx);
        }

        return new TransactionTemplate<Long>() {
            @Override
            public Long execute(Transaction tx) throws Exception {
                return get(tx);
            }
        }.execute();
    }

    @Override
    public void set(Transaction tx, long newValue) {
        AlphaTransaction alphaTx = (AlphaTransaction) tx;
        AlphaProgrammaticLongTranlocal tranlocal = (AlphaProgrammaticLongTranlocal) alphaTx.openForWrite(this);
        tranlocal.value = newValue;
    }

    public void set(final long newValue) {
        Transaction tx = getThreadLocalTransaction();
        if (tx != null && tx.getStatus() == TransactionStatus.active) {
            set(tx, newValue);
            return;
        }

        new TransactionTemplate() {
            @Override
            public Long execute(Transaction tx) throws Exception {
                set(tx, newValue);
                return null;
            }
        }.execute();
    }

    @Override
    public long inc(Transaction tx, long amount) {
        AlphaTransaction alphaTx = (AlphaTransaction) tx;
        AlphaProgrammaticLongTranlocal tranlocal = (AlphaProgrammaticLongTranlocal) alphaTx.openForWrite(this);
        tranlocal.value += amount;
        return tranlocal.value;
    }

    public long inc(final long amount) {
        return new TransactionTemplate<Long>() {
            @Override
            public Long execute(Transaction tx) throws Exception {
                return inc(tx, amount);
            }
        }.execute();
    }

    @Override
    public void commutingInc(final long amount) {
        Transaction tx = getThreadLocalTransaction();
        if (tx != null && tx.getStatus() == TransactionStatus.active) {
            commutingInc(tx, amount);
            return;
        }

        new TransactionTemplate() {
            @Override
            public Long execute(Transaction tx) throws Exception {
                commutingInc(tx, amount);
                return null;
            }
        }.execute();
    }

    @Override
    public void commutingInc(Transaction tx, long amount) {
        AlphaTransaction alphaTx = (AlphaTransaction) tx;
        AlphaProgrammaticLongTranlocal tranlocal = (AlphaProgrammaticLongTranlocal) alphaTx.openForCommutingWrite(this);

        if (tranlocal.isCommuting()) {
            tranlocal.pendingIncrements += amount;
        } else {
            tranlocal.value += amount;
        }
    }

    @Override
    public AlphaTranlocal ___openForCommutingOperation() {
        return new AlphaProgrammaticLongTranlocal(this, true);
    }

    @Override
    public AlphaTranlocal ___openUnconstructed() {
        return new AlphaProgrammaticLongTranlocal(this, false);
    }
}

class AlphaProgrammaticLongTranlocal extends AlphaTranlocal {

    public long value;
    public long pendingIncrements;

    private final AlphaProgrammaticLong transactionalObject;
    private AlphaProgrammaticLongTranlocal origin;

    public AlphaProgrammaticLongTranlocal(AlphaProgrammaticLong transactionalObject, boolean commuting) {
        this.transactionalObject = transactionalObject;
        this.___writeVersion = commuting ? -2 : 0;
    }

    public AlphaProgrammaticLongTranlocal(AlphaProgrammaticLongTranlocal origin) {
        this.origin = origin;
        this.transactionalObject = origin.transactionalObject;
        this.value = origin.value;
    }

    @Override
    public void fixatePremature(AlphaTransaction tx, AlphaTranlocal origin) {
        if (!isCommuting()) {
            return;
        }

        //System.out.println("premature fixation");

        this.origin = (AlphaProgrammaticLongTranlocal) origin;
        this.value = this.origin.value;
        this.value += pendingIncrements;
        this.pendingIncrements = 0;
        //-1 indicates that it is a normaly dirty that needs writing.
        this.___writeVersion = -1;
    }

    @Override
    public void ifCommutingThenFixate(AlphaTransaction tx) {
        if (!isCommuting()) {
            return;
        }

        AlphaProgrammaticLongTranlocal tranlocal = (AlphaProgrammaticLongTranlocal) transactionalObject.___load();
        if (tranlocal == null) {
            throw new UncommittedReadConflict();
        }

        //System.out.println("late fixation");

        this.value = tranlocal.value;
        this.value += pendingIncrements;
        this.pendingIncrements = 0;

        //-1 indicates that it is a normaly dirty that needs writing.
        this.___writeVersion = -1;
    }

    @Override
    public AlphaTranlocal getOrigin() {
        return origin;
    }

    @Override
    public AlphaTransactionalObject getTransactionalObject() {
        return transactionalObject;
    }

    @Override
    public AlphaTranlocalSnapshot takeSnapshot() {
        throw new TodoException();
    }

    @Override
    public AlphaTranlocal openForWrite() {
        return new AlphaProgrammaticLongTranlocal(this);
    }

    @Override
    public void prepareForCommit(long writeVersion) {
        origin = null;
        ___writeVersion = writeVersion;
    }

    @Override
    public boolean isDirty() {
        if (isCommitted()) {
            return false;
        }

        if (origin == null) {
            return true;
        }

        if (isCommuting()) {
            return pendingIncrements != 0;
        }

        return origin.value != value;
    }
}