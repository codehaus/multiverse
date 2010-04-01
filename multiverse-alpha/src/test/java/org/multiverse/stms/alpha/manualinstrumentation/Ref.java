package org.multiverse.stms.alpha.manualinstrumentation;

import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTranlocalSnapshot;
import org.multiverse.stms.alpha.mixins.DefaultTxObjectMixin;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.templates.TransactionTemplate;

import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

/**
 * This implementation suffers from the ABA problem (well.. the stm suffers from it because the isDirty method suffers
 * from it). This can be fixed very easily, just add a counter. So although the primitives may not have changed in the
 * end but the counter has. And this will cause the writeconfict we are after for the ABA problem.  See the TransactionalAbaReference.
 *
 * @author Peter Veentjer
 */
public class Ref<E> extends DefaultTxObjectMixin {

    public Ref() {
        new TransactionTemplate() {
            @Override
            public Object execute(Transaction t) throws Exception {
                RefTranlocal<E> tranlocal = (RefTranlocal) ((AlphaTransaction) t).openForConstruction(Ref.this);
                return null;
            }
        }.execute();
    }

    public Ref(final E value) {
        new TransactionTemplate() {
            @Override
            public Object execute(Transaction t) throws Exception {
                RefTranlocal<E> tranlocal = (RefTranlocal) ((AlphaTransaction) t).openForConstruction(Ref.this);
                tranlocal.value = value;
                return null;
            }
        }.execute();
    }

    private final static TransactionFactory getTxFactory = getGlobalStmInstance()
            .getTransactionFactoryBuilder()
            .setReadonly(false)
            .setAutomaticReadTracking(false).build();

    public E get() {
        return new TransactionTemplate<E>(getTxFactory) {
            @Override
            public E execute(Transaction t) throws Exception {
                RefTranlocal<E> tranlocal = (RefTranlocal) ((AlphaTransaction) t).openForRead(Ref.this);
                return get(tranlocal);
            }
        }.execute();
    }

    public void set(final E newValue) {
        new TransactionTemplate() {
            @Override
            public Object execute(Transaction t) throws Exception {
                RefTranlocal<E> tranlocal = (RefTranlocal) ((AlphaTransaction) t).openForWrite(Ref.this);
                set(tranlocal, newValue);
                return null;
            }
        }.execute();
    }

    private final static TransactionFactory isNullTxFactory = getGlobalStmInstance()
            .getTransactionFactoryBuilder()
            .setReadonly(true)
            .setAutomaticReadTracking(false).build();


    public boolean isNull() {
        return new TransactionTemplate<Boolean>(isNullTxFactory) {
            @Override
            public Boolean execute(Transaction t) throws Exception {
                RefTranlocal<E> tranlocal = (RefTranlocal) ((AlphaTransaction) t).openForRead(Ref.this);
                return isNull(tranlocal);
            }
        }.execute();
    }

    public E clear() {
        return new TransactionTemplate<E>() {
            @Override
            public E execute(Transaction t) throws Exception {
                RefTranlocal<E> tranlocal = (RefTranlocal) ((AlphaTransaction) t).openForWrite(Ref.this);
                return clear(tranlocal);
            }
        }.execute();
    }

    @Override
    public RefTranlocal<E> ___openUnconstructed() {
        return new RefTranlocal<E>(this);
    }

    public E clear(RefTranlocal<E> tranlocal) {
        E oldValue = tranlocal.value;
        tranlocal.value = null;
        return oldValue;
    }

    public boolean isNull(RefTranlocal<E> tranlocal) {
        return tranlocal.value == null;
    }

    public E get(RefTranlocal<E> tranlocal) {
        return tranlocal.value;
    }

    public void set(RefTranlocal<E> tranlocal, E newValue) {
        if (tranlocal.isCommitted()) {
            throw new ReadonlyException();
        }
        tranlocal.value = newValue;
    }
}

class RefTranlocal<E> extends AlphaTranlocal {

    E value;


    RefTranlocal(RefTranlocal<E> origin) {
        this.___transactionalObject = origin.___transactionalObject;
        this.___origin = origin;
        this.value = origin.value;
    }

    RefTranlocal(Ref<E> txObject) {
        this.___transactionalObject = txObject;
    }

    @Override
    public AlphaTranlocal openForWrite() {
        return new RefTranlocal<E>(this);
    }

    @Override
    public void prepareForCommit(long writeVersion) {
        this.___writeVersion = writeVersion;
        this.___origin = null;
    }

    @Override
    public AlphaTranlocalSnapshot takeSnapshot() {
        return new RefTranlocalSnapshot<E>(this);
    }

    @Override
    public boolean isDirty() {
        if (isCommitted()) {
            return false;
        }

        if (___origin == null) {
            return true;
        }

        RefTranlocal origin = (RefTranlocal) ___origin;
        if (origin.value != value) {
            return true;
        }

        return false;
    }
}

class RefTranlocalSnapshot<E> extends AlphaTranlocalSnapshot {

    final RefTranlocal ___tranlocal;
    final E value;

    RefTranlocalSnapshot(RefTranlocal<E> tranlocal) {
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
