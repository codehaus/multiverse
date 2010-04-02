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
 * DefaultTransactionalReference that fixes the Aba problem.
 * <p/>
 * See the DefaultTransactionalReference for more documentation about the Aba problem.
 *
 * @author Peter Veentjer
 */
public class AbaRef<E> extends DefaultTxObjectMixin {

    public AbaRef() {
        new TransactionTemplate() {
            @Override
            public Object execute(Transaction t) throws Exception {
                AbaRefTranlocal<E> tranlocal = (AbaRefTranlocal) ((AlphaTransaction) t).openForConstruction(AbaRef.this);
                return null;
            }
        }.execute();
    }

    public AbaRef(final E value) {
        new TransactionTemplate() {
            @Override
            public Object execute(Transaction t) throws Exception {
                AbaRefTranlocal<E> tranlocal = (AbaRefTranlocal) ((AlphaTransaction) t).openForConstruction(AbaRef.this);
                tranlocal.value = value;
                return null;
            }
        }.execute();
    }

    private final static TransactionFactory getTxFactory = getGlobalStmInstance()
            .getTransactionFactoryBuilder()
            .setSpeculativeConfigurationEnabled(false)
            .setReadonly(true)
            .setAutomaticReadTracking(false)
            .build();

    public E get() {
        return new TransactionTemplate<E>(getTxFactory) {
            @Override
            public E execute(Transaction transaction) throws Exception {
                AlphaTransaction t = (AlphaTransaction) transaction;
                AbaRefTranlocal tranlocal = (AbaRefTranlocal) t.openForRead(AbaRef.this);
                return get(tranlocal);
            }
        }.execute();
    }

    private final static TransactionFactory setTxFactory = getGlobalStmInstance()
            .getTransactionFactoryBuilder()
            .setSpeculativeConfigurationEnabled(false)
            .setReadonly(false)
            .setAutomaticReadTracking(false)
            .build();

    public void set(final E newValue) {
        new TransactionTemplate<E>(setTxFactory) {
            @Override
            public E execute(Transaction t) throws Exception {
                AbaRefTranlocal<E> tranlocal = (AbaRefTranlocal) ((AlphaTransaction) t).openForWrite(AbaRef.this);
                set(tranlocal, newValue);
                return null;
            }
        }.execute();
    }

    private final static TransactionFactory isNullTxFactory = getGlobalStmInstance().getTransactionFactoryBuilder()
            .setReadonly(true)
            .setAutomaticReadTracking(false).build();


    public boolean isNull() {
        return new TransactionTemplate<Boolean>(isNullTxFactory) {
            @Override
            public Boolean execute(Transaction transaction) throws Exception {
                AlphaTransaction t = (AlphaTransaction) transaction;
                AbaRefTranlocal tranlocal = (AbaRefTranlocal) t.openForRead(AbaRef.this);
                return isNull(tranlocal);
            }
        }.execute();
    }

    public E clear() {
        return new TransactionTemplate<E>() {
            @Override
            public E execute(Transaction t) throws Exception {
                AbaRefTranlocal<E> tranlocal = (AbaRefTranlocal) ((AlphaTransaction) t).openForWrite(AbaRef.this);
                return clear(tranlocal);
            }
        }.execute();
    }

    @Override
    public AbaRefTranlocal<E> ___openUnconstructed() {
        return new AbaRefTranlocal<E>(this);
    }

    public E clear(AbaRefTranlocal<E> tranlocal) {
        E oldValue = tranlocal.value;
        set(tranlocal, null);
        return oldValue;
    }

    public boolean isNull(AbaRefTranlocal<E> tranlocal) {
        return tranlocal.value == null;
    }

    public E get(AbaRefTranlocal<E> tranlocal) {
        return tranlocal.value;
    }

    public void set(AbaRefTranlocal<E> tranlocal, E newValue) {
        if (tranlocal.isCommitted()) {
            throw new ReadonlyException();
        }
        tranlocal.value = newValue;
        tranlocal.writeVersion++;
    }
}

class AbaRefTranlocal<E> extends AlphaTranlocal {

    E value;
    long writeVersion;

    AbaRefTranlocal(AbaRefTranlocal<E> origin) {
        this.___transactionalObject = origin.___transactionalObject;
        this.___origin = origin;
        this.value = origin.value;
        this.writeVersion = origin.writeVersion;
    }

    AbaRefTranlocal(AbaRef<E> txObject) {
        this(txObject, null);
    }

    AbaRefTranlocal(AbaRef<E> owner, E value) {
        this.___transactionalObject = owner;
        this.value = value;
        this.writeVersion = Long.MIN_VALUE;
    }

    @Override
    public AlphaTranlocal openForWrite() {
        return new AbaRefTranlocal<E>(this);
    }

    @Override
    public AlphaTranlocalSnapshot takeSnapshot() {
        return new AbaRefTranlocalSnapshot<E>(this);
    }

    @Override
    public boolean isDirty() {
        if (isCommitted()) {
            return false;
        }

        if (___origin == null) {
            return true;
        }

        AbaRefTranlocal origin = (AbaRefTranlocal) ___origin;

        if (origin.value != value) {
            return true;
        }

        if (origin.writeVersion != writeVersion) {
            return true;
        }

        return false;
    }
}

class AbaRefTranlocalSnapshot<E> extends AlphaTranlocalSnapshot {

    final AbaRefTranlocal<E> ___tranlocal;
    final E value;
    final long writeVersion;

    AbaRefTranlocalSnapshot(AbaRefTranlocal<E> tranlocalAbaRef) {
        this.___tranlocal = tranlocalAbaRef;
        this.value = tranlocalAbaRef.value;
        this.writeVersion = tranlocalAbaRef.writeVersion;
    }

    @Override
    public AlphaTranlocal getTranlocal() {
        return ___tranlocal;
    }

    @Override
    public void restore() {
        ___tranlocal.writeVersion = writeVersion;
        ___tranlocal.value = value;
    }
}
