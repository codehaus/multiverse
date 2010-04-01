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
import static org.multiverse.api.StmUtils.retry;

public final class Stack<E> extends DefaultTxObjectMixin {

    public Stack() {
        new TransactionTemplate() {
            @Override
            public Object execute(Transaction t) {
                StackTranlocal<E> tranlocal = (StackTranlocal<E>) ((AlphaTransaction) t).openForConstruction(Stack.this);
                return null;
            }
        }.execute();
    }

    private final static TransactionFactory sizeTxFactory = getGlobalStmInstance()
            .getTransactionFactoryBuilder()
            .setReadonly(true)
            .setAutomaticReadTracking(false).build();


    public int size() {
        return new TransactionTemplate<Integer>(sizeTxFactory) {
            @Override
            public Integer execute(Transaction transaction) {
                AlphaTransaction t = (AlphaTransaction) transaction;
                StackTranlocal tranlocal = (StackTranlocal) t.openForRead(Stack.this);
                return size(tranlocal);
            }
        }.execute();
    }

    private final static TransactionFactory isEmptyTxFactory = getGlobalStmInstance()
            .getTransactionFactoryBuilder()
            .setReadonly(true)
            .setAutomaticReadTracking(false).build();


    public boolean isEmpty() {
        return new TransactionTemplate<Boolean>(isEmptyTxFactory) {
            @Override
            public Boolean execute(Transaction transaction) {
                AlphaTransaction t = (AlphaTransaction) transaction;
                StackTranlocal tranlocal = (StackTranlocal) t.openForRead(Stack.this);
                return isEmpty(tranlocal);
            }
        }.execute();
    }

    public void push(final E item) {
        new TransactionTemplate() {
            @Override
            public Integer execute(Transaction t) {
                StackTranlocal tranlocal = (StackTranlocal) ((AlphaTransaction) t).openForWrite(Stack.this);
                push(tranlocal, item);
                return null;
            }
        }.execute();
    }

    public E pop() {
        return new TransactionTemplate<E>() {
            @Override
            public E execute(Transaction t) {
                StackTranlocal<E> tranlocal = (StackTranlocal) ((AlphaTransaction) t).openForWrite(Stack.this);
                return pop(tranlocal);
            }
        }.execute();
    }

    public void clear() {
        new TransactionTemplate() {
            @Override
            public Integer execute(Transaction t) {
                StackTranlocal tranlocal = (StackTranlocal) ((AlphaTransaction) t).openForWrite(Stack.this);
                clear(tranlocal);
                return null;
            }
        }.execute();
    }

    @Override
    public AlphaTranlocal ___openUnconstructed() {
        return new StackTranlocal<E>(this);
    }

    public void clear(StackTranlocal<E> tranlocal) {
        if (tranlocal.isCommitted()) {
            throw new ReadonlyException();
        }

        tranlocal.size = 0;
        tranlocal.head = null;
    }

    public void push(StackTranlocal<E> tranlocal, E item) {
        if (tranlocal.isCommitted()) {
            throw new ReadonlyException();
        }

        if (item == null) {
            throw new NullPointerException();
        }

        tranlocal.head = new Node<E>(tranlocal.head, item);
        tranlocal.size++;
    }

    public E pop(StackTranlocal<E> tranlocal) {
        if (tranlocal.isCommitted()) {
            throw new ReadonlyException();
        }

        if (tranlocal.size == 0) {
            retry();
        }

        tranlocal.size--;
        Node<E> oldHead = tranlocal.head;
        tranlocal.head = oldHead.next;
        return oldHead.value;
    }

    public boolean isEmpty(StackTranlocal<E> tranlocal) {
        return tranlocal.size == 0;
    }

    public int size(StackTranlocal<E> tranlocal) {
        return tranlocal.size;
    }

    public static class Node<E> {

        final Node<E> next;
        final E value;

        Node(Node<E> next, E value) {
            this.next = next;
            this.value = value;
        }
    }

    public static final class StackTranlocal<E> extends AlphaTranlocal {

        int size;
        Node<E> head;

        /**
         * Makes an initial version.
         *
         * @param txObject
         */
        StackTranlocal(Stack<E> txObject) {
            this.___transactionalObject = txObject;
        }

        /**
         * Makes a clone.
         *
         * @param origin
         */
        StackTranlocal(StackTranlocal<E> origin) {
            this.___origin = origin;
            this.___transactionalObject = origin.___transactionalObject;
            this.size = origin.size;
            this.head = origin.head;
        }

        @Override
        public AlphaTranlocal openForWrite() {
            return new StackTranlocal<E>(this);
        }

        @Override
        public void prepareForCommit(long writeVersion) {
            this.___writeVersion = writeVersion;
            this.___origin = null;
        }

        @Override
        public AlphaTranlocalSnapshot takeSnapshot() {
            throw new RuntimeException();
        }

        @Override
        public boolean isDirty() {
            if (isCommitted()) {
                return false;
            }

            if (___origin == null) {
                return true;
            }

            StackTranlocal origin = (StackTranlocal) ___origin;
            if (origin.head != head) {
                return true;
            }

            return false;
        }
    }
}

