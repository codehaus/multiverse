package org.multiverse.stms.alpha.manualinstrumentation;

import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.mixins.BasicMixin;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.templates.TransactionTemplate;

import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.retry;

public class IntRef extends BasicMixin {

    public static IntRef createUncommitted() {
        return new IntRef(String.class);
    }

    public IntRef() {
        this(0);
    }

    public IntRef(AlphaStm stm) {
        this(stm, 0);
    }

    public IntRef(AlphaStm stm, final int value) {
        new TransactionTemplate(stm) {
            @Override
            public Object execute(Transaction t) {
                IntRefTranlocal tranlocal = (IntRefTranlocal) ((AlphaTransaction) t).openForConstruction(IntRef.this);
                tranlocal.value = value;
                return null;
            }
        }.execute();
    }

    public IntRef(AlphaTransaction t, final int value) {
        IntRefTranlocal tranlocal = (IntRefTranlocal) ((AlphaTransaction) t).openForConstruction(IntRef.this);
        tranlocal.value = value;
    }

    public IntRef(final int value) {
        new TransactionTemplate() {
            @Override
            public Object execute(Transaction t) {
                IntRefTranlocal tranlocal = (IntRefTranlocal) ((AlphaTransaction) t).openForConstruction(IntRef.this);
                tranlocal.value = value;
                return null;
            }
        }.execute();
    }

    //this constructor is used for creating an uncommitted IntValue, class is used to prevent
    //overloading problems
    private IntRef(Class someClass) {
    }

    public void await(final int expectedValue) {
        new TransactionTemplate() {
            @Override
            public Object execute(Transaction t) {
                IntRefTranlocal tranlocal = (IntRefTranlocal) ((AlphaTransaction) t).openForWrite(IntRef.this);
                await(tranlocal, expectedValue);
                return null;
            }
        }.execute();
    }

    public void set(final int value) {
        new TransactionTemplate() {
            @Override
            public Object execute(Transaction t) {
                IntRefTranlocal tranlocal = (IntRefTranlocal) ((AlphaTransaction) t).openForWrite(IntRef.this);
                set(tranlocal, value);
                return null;
            }
        }.execute();
    }

    private final static TransactionFactory getTxFactory = getGlobalStmInstance().getTransactionFactoryBuilder()
            .setReadonly(true).build();


    public int get() {
        return new TransactionTemplate<Integer>(getTxFactory) {
            @Override
            public Integer execute(Transaction t) {
                IntRefTranlocal tranlocal = (IntRefTranlocal) ((AlphaTransaction) t).openForRead(IntRef.this);
                return get(tranlocal);
            }
        }.execute();
    }

    public void loopInc(final int amount) {
        new TransactionTemplate() {
            @Override
            public Integer execute(Transaction t) {
                IntRefTranlocal tranlocal = (IntRefTranlocal) ((AlphaTransaction) t).openForWrite(IntRef.this);
                loopInc(tranlocal, amount);
                return null;
            }
        }.execute();
    }

    public void inc() {
        new TransactionTemplate() {
            @Override
            public Integer execute(Transaction t) {
                IntRefTranlocal tranlocal = (IntRefTranlocal) ((AlphaTransaction) t).openForWrite(IntRef.this);
                inc(tranlocal);
                return null;
            }
        }.execute();
    }

    public void inc(AlphaStm stm) {
        new TransactionTemplate(stm) {
            @Override
            public Integer execute(Transaction t) {
                IntRefTranlocal tranlocal = (IntRefTranlocal) ((AlphaTransaction) t).openForWrite(IntRef.this);
                inc(tranlocal);
                return null;
            }
        }.execute();
    }


    public void dec() {
        new TransactionTemplate() {
            @Override
            public Integer execute(Transaction t) {
                IntRefTranlocal tranlocal = (IntRefTranlocal) ((AlphaTransaction) t).openForWrite(IntRef.this);
                dec(tranlocal);
                return null;
            }
        }.execute();
    }

    @Override
    public IntRefTranlocal ___openUnconstructed() {
        return new IntRefTranlocal(this);
    }

    public void loopInc(IntRefTranlocal tranlocal, int amount) {
        if (tranlocal.isCommitted()) {
            throw new ReadonlyException();
        }

        for (int k = 0; k < amount; k++) {
            inc(tranlocal);
        }
    }

    public void set(IntRefTranlocal tranlocal, int newValue) {
        if (tranlocal.isCommitted()) {
            throw new ReadonlyException();
        }
        tranlocal.value = newValue;
    }

    public int get(IntRefTranlocal tranlocal) {
        return tranlocal.value;
    }

    public int get(AlphaTransaction tx) {
        return ((IntRefTranlocal) tx.openForRead(this)).value;
    }


    public void inc(IntRefTranlocal tranlocal) {
        if (tranlocal.isCommitted()) {
            throw new ReadonlyException();
        }
        tranlocal.value++;
    }

    public void inc(AlphaTransaction tx) {
        inc((IntRefTranlocal) tx.openForWrite(this));
    }

    public void dec(IntRefTranlocal tranlocal) {
        if (tranlocal.isCommitted()) {
            throw new ReadonlyException();
        }
        tranlocal.value--;
    }

    public void await(IntRefTranlocal tranlocal, int expectedValue) {
        if (tranlocal.value != expectedValue) {
            retry();
        }
    }
}

