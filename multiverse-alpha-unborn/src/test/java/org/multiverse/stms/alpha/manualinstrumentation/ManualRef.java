package org.multiverse.stms.alpha.manualinstrumentation;

import org.multiverse.api.Listeners;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.mixins.DefaultTxObjectMixin;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.templates.TransactionTemplate;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ManualRef extends DefaultTxObjectMixin {

    private List<Transaction> lockTransactions = new LinkedList<Transaction>();
    private List<Transaction> releaseLockTransactions = new LinkedList<Transaction>();

    public static ManualRef createUncommitted() {
        return new ManualRef(String.class);
    }

    public ManualRef(AlphaStm stm) {
        this(stm, 0);
    }

    public ManualRef(AlphaStm stm, final int value) {
        new TransactionTemplate(stm.getTransactionFactoryBuilder().build(), false, false) {
            @Override
            public Object execute(Transaction t) {
                ManualRefTranlocal tranlocal = (ManualRefTranlocal) ((AlphaTransaction) t).openForConstruction(ManualRef.this);
                tranlocal.value = value;
                return null;
            }
        }.execute();
    }

    public ManualRef(AlphaTransaction tx, final int value) {
        ManualRefTranlocal tranlocal = (ManualRefTranlocal) tx.openForConstruction(ManualRef.this);
        tranlocal.value = value;
    }

    //this constructor is used for creating an uncommitted IntValue, class is used to prevent
    //overloading problems
    private ManualRef(Class someClass) {
    }

    public int get(AlphaStm stm) {
        TransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setReadonly(true)
                .build();
        return get(txFactory);
    }

    public int get(TransactionFactory txFactory) {
        return new TransactionTemplate<Integer>(txFactory, false, false) {
            @Override
            public Integer execute(Transaction t) throws Exception {
                return get((AlphaTransaction) t);
            }
        }.execute();
    }

    public int get(AlphaTransaction t) {
        ManualRefTranlocal tranlocal = (ManualRefTranlocal) t.openForRead(this);
        return tranlocal.value;
    }

    public void inc(AlphaStm stm) {
        TransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setReadonly(false)
                .build();
        inc(txFactory);
    }

    public void inc(TransactionFactory txFactory) {
        new TransactionTemplate(txFactory, false, false) {
            @Override
            public Object execute(Transaction t) {
                inc((AlphaTransaction) t);
                return null;
            }
        }.execute();
    }

    public void inc(AlphaTransaction tx) {
        ManualRefTranlocal tranlocal = (ManualRefTranlocal) tx.openForWrite(this);
        tranlocal.value++;
    }

    public void inc(AlphaTransaction tx, int value) {
        ManualRefTranlocal tranlocal = (ManualRefTranlocal) tx.openForWrite(this);
        tranlocal.value += value;
    }

    public void set(AlphaStm stm, int value) {
        TransactionFactory factory = stm.getTransactionFactoryBuilder()
                .setReadonly(false)
                .build();
        set(factory, value);
    }

    public void set(TransactionFactory txFactory, final int value) {
        new TransactionTemplate(txFactory, false, false) {
            @Override
            public Object execute(Transaction t) throws Exception {
                set((AlphaTransaction) t, value);
                return null;
            }
        }.execute();
    }

    public void set(AlphaTransaction tx, int value) {
        ManualRefTranlocal tranlocal = (ManualRefTranlocal) tx.openForWrite(this);
        tranlocal.value = value;
    }

    @Override
    public ManualRefTranlocal ___openUnconstructed() {
        return new ManualRefTranlocal(this);
    }

    @Override
    public Listeners ___storeUpdate(AlphaTranlocal tranlocal, long writeVersion, boolean releaseLock) {
        if (releaseLock) {
            releaseLockTransactions.add(___getLockOwner());
        }
        return super.___storeUpdate(tranlocal, writeVersion, releaseLock);
    }

    @Override
    public boolean ___tryLock(Transaction lockOwner) {
        boolean success = super.___tryLock(lockOwner);
        lockTransactions.add(lockOwner);
        return success;
    }

    @Override
    public void ___releaseLock(Transaction expectedLockOwner) {
        releaseLockTransactions.add(expectedLockOwner);
        super.___releaseLock(expectedLockOwner);
    }

    public void resetLockInfo() {
        lockTransactions.clear();
        releaseLockTransactions.clear();
    }

    public void assertNoLockAcquired() {
        assertTrue(lockTransactions.isEmpty());
    }

    public void assertLockAcquired() {
        assertFalse(lockTransactions.isEmpty());
    }

    public void assertNoLocksReleased() {
        assertTrue(releaseLockTransactions.isEmpty());
    }

    public void assertLockReleased() {
        assertFalse(releaseLockTransactions.isEmpty());
    }
}

