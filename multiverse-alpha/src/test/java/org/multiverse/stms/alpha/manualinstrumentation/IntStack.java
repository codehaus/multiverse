package org.multiverse.stms.alpha.manualinstrumentation;

import org.multiverse.api.GlobalStmInstance;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.manualinstrumentation.IntStackTranlocal.IntNode;
import org.multiverse.stms.alpha.mixins.DefaultTxObjectMixin;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.templates.TransactionTemplate;

import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.retry;

public final class IntStack extends DefaultTxObjectMixin {

    public IntStack() {
        new TransactionTemplate() {
            @Override
            public Object execute(Transaction t) {
                IntStackTranlocal tranlocal = (IntStackTranlocal) ((AlphaTransaction) t).openForWrite(IntStack.this);
                return null;
            }
        }.execute();
    }

    private final static TransactionFactory sizeTxFactory = getGlobalStmInstance().getTransactionFactoryBuilder()
            .setReadonly(true)
            .setAutomaticReadTracking(false).build();


    public int size() {
        return new TransactionTemplate<Integer>(sizeTxFactory) {
            @Override
            public Integer execute(Transaction transaction) {
                return size((AlphaTransaction) transaction);
            }
        }.execute();
    }

    public int size(AlphaTransaction t) {
        IntStackTranlocal tranlocal = (IntStackTranlocal) t.openForRead(IntStack.this);
        return tranlocal.size;
    }

    private final static TransactionFactory isEmptyTxFactory = getGlobalStmInstance().getTransactionFactoryBuilder()
            .setReadonly(true)
            .setAutomaticReadTracking(false).build();


    public boolean isEmpty() {
        return new TransactionTemplate<Boolean>(isEmptyTxFactory) {
            @Override
            public Boolean execute(Transaction transaction) {
                AlphaTransaction t = (AlphaTransaction) transaction;
                IntStackTranlocal tranlocal = (IntStackTranlocal) t.openForRead(IntStack.this);
                return tranlocal.size == 0;
            }
        }.execute();
    }

    public final static TransactionFactory popTxFactory = GlobalStmInstance.getGlobalStmInstance().getTransactionFactoryBuilder().
            setReadonly(false).
            setSmartTxLengthSelector(true).
            setPreventWriteSkew(false).
            setFamilyName("IntStack.pop()").
            setAutomaticReadTracking(true).build();

    public int pop() {
        return new TransactionTemplate<Integer>(popTxFactory) {
            @Override
            public Integer execute(Transaction t) {
                return pop((AlphaTransaction) t);
            }
        }.execute();
    }

    public int pop(AlphaTransaction t) {
        IntStackTranlocal tranlocal = (IntStackTranlocal) t.openForWrite(this);

        if (tranlocal.head == null) {
            retry();
        }

        tranlocal.size--;
        IntNode oldHead = tranlocal.head;
        tranlocal.head = oldHead.next;
        return oldHead.value;
    }

    public final static TransactionFactory pushTxFactory = GlobalStmInstance.getGlobalStmInstance().getTransactionFactoryBuilder().
            setReadonly(false).
            setSmartTxLengthSelector(true).
            setPreventWriteSkew(false).
            setFamilyName("IntStack.push()").
            setAutomaticReadTracking(false).build();


    public void push(final int value) {
        new TransactionTemplate(pushTxFactory) {
            @Override
            public Integer execute(Transaction t) {
                push((AlphaTransaction) t, value);
                return null;
            }
        }.execute();
    }

    public void push(AlphaTransaction t, int value) {
        IntStackTranlocal tranlocal = (IntStackTranlocal) t.openForWrite(this);
        push(tranlocal, value);
    }

    public void push(IntStackTranlocal tranlocal, int value) {
        if (tranlocal.isCommitted()) {
            throw new ReadonlyException();
        }

        tranlocal.head = new IntNode(value, tranlocal.head);
        tranlocal.size++;
    }

    @Override
    public AlphaTranlocal ___openUnconstructed() {
        return new IntStackTranlocal(this);
    }
}

