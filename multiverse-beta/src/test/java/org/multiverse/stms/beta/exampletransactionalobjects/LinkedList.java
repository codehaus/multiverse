package org.multiverse.stms.beta.exampletransactionalobjects;

import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.api.functions.Function;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.Listeners;
import org.multiverse.stms.beta.transactionalobjects.AbstractBetaTransactionalObject;
import org.multiverse.stms.beta.transactionalobjects.BetaTranlocal;
import org.multiverse.stms.beta.transactionalobjects.BetaTransactionalObject;
import org.multiverse.stms.beta.transactions.BetaTransaction;

public class LinkedList<E> extends AbstractBetaTransactionalObject {

    public LinkedList(BetaTransaction tx) {
        super(tx);
    }

    @Override
    public Listeners ___commitAll(BetaTranlocal tranlocal, BetaTransaction tx, BetaObjectPool pool) {
        throw new TodoException();
    }

    @Override
    public BetaTranlocal ___newTranlocal() {
        throw new TodoException();
    }

    @Override
    public boolean ___load(int spinCount, BetaTransaction newLockOwner, int lockMode, BetaTranlocal tranlocal) {
        throw new TodoException();
    }

    @Override
    public void ensure() {
        throw new TodoException();
    }

    @Override
    public void ensure(Transaction tx) {
        throw new TodoException();
    }

    @Override
    public void privatize() {
        throw new TodoException();
    }

    @Override
    public void privatize(Transaction tx) {
        throw new TodoException();
    }

    @Override
    public void deferredEnsure() {
        throw new TodoException();
    }

    @Override
    public void deferredEnsure(Transaction tx) {
        throw new TodoException();
    }

    @Override
    public Listeners ___commitDirty(BetaTranlocal tranlocal, BetaTransaction tx, BetaObjectPool pool) {
        throw new TodoException();
    }

    public E removeFromFront(BetaTransaction tx) {
        LinkedListTranlocal<E> tranlocal = (LinkedListTranlocal) tx.openForWrite(
                this, LOCKMODE_NONE);
        if (tranlocal.head == null) {
            return null;
        }

        if (tranlocal.head == tranlocal.tail) {
            LinkedListNodeTranlocal<E> nodeTranlocal = (LinkedListNodeTranlocal<E>) tx.openForRead(
                    tranlocal.head, LOCKMODE_NONE);
            tranlocal.head = null;
            tranlocal.tail = null;
            return nodeTranlocal.value;
        } else {
            LinkedListNodeTranlocal<E> headTranlocal = (LinkedListNodeTranlocal<E>) tx.openForWrite(
                    tranlocal.head, LOCKMODE_NONE);

            LinkedListNodeTranlocal<E> nextHeadTranlocal = (LinkedListNodeTranlocal<E>) tx.openForWrite(
                    headTranlocal.next, LOCKMODE_NONE);

            nextHeadTranlocal.prev = null;
            tranlocal.head = (LinkedListNode<E>) nextHeadTranlocal.owner;
            return headTranlocal.value;
        }
    }

    public E removeFromBack(BetaTransaction tx) {
        LinkedListTranlocal tranlocal = (LinkedListTranlocal) tx.openForWrite(this, LOCKMODE_NONE);
        if (tranlocal.head == null) {
            return null;
        }

        if (tranlocal.head == tranlocal.tail) {
            LinkedListNodeTranlocal<E> nodeTranlocal = (LinkedListNodeTranlocal<E>) tx.openForRead(
                    tranlocal.head, LOCKMODE_NONE);
            tranlocal.head = null;
            tranlocal.tail = null;
            return nodeTranlocal.value;
        } else {
            LinkedListNodeTranlocal<E> tailTranlocal = (LinkedListNodeTranlocal<E>) tx.openForWrite(
                    tranlocal.tail, LOCKMODE_NONE);

            LinkedListNodeTranlocal<E> prevTailTranlocal = (LinkedListNodeTranlocal<E>) tx.openForWrite(
                    tailTranlocal.prev, LOCKMODE_NONE);

            prevTailTranlocal.next = null;
            tranlocal.tail = (LinkedListNode) prevTailTranlocal.owner;
            return tailTranlocal.value;
        }
    }

    public void addInFront(BetaTransaction tx, E item) {
        LinkedListTranlocal tranlocal = (LinkedListTranlocal) tx.openForWrite(this, LOCKMODE_NONE);
        LinkedListNode newNode = new LinkedListNode(tx);
        LinkedListNodeTranlocal newNodeTranlocal = (LinkedListNodeTranlocal) tx.openForConstruction(newNode);
        newNodeTranlocal.value = item;

        if (tranlocal.head == null) {
            tranlocal.head = newNode;
            tranlocal.tail = newNode;
        } else {
            LinkedListNodeTranlocal<E> headTranlocal = (LinkedListNodeTranlocal) tx.openForWrite(
                    tranlocal.head, LOCKMODE_NONE);

            headTranlocal.prev = newNode;
            newNodeTranlocal.next = tranlocal.head;
            tranlocal.head = newNode;
        }
    }

    public void addInBack(BetaTransaction tx, E item) {
        LinkedListTranlocal tranlocal = (LinkedListTranlocal) tx.openForWrite(this, LOCKMODE_NONE);
        LinkedListNode newNode = new LinkedListNode(tx);

        LinkedListNodeTranlocal<E> newNodeTranlocal = (LinkedListNodeTranlocal) tx.openForConstruction(newNode);
        newNodeTranlocal.value = item;

        if (tranlocal.head == null) {
            tranlocal.head = newNode;
            tranlocal.tail = newNode;
        } else {
            LinkedListNodeTranlocal<E> tailTranlocal = (LinkedListNodeTranlocal) tx.openForWrite(
                    tranlocal.tail, LOCKMODE_NONE);

            tailTranlocal.next = newNode;
            newNodeTranlocal.prev = tranlocal.tail;
            tranlocal.tail = newNode;
        }
    }

    @Override
    public String toDebugString() {
        throw new RuntimeException();
    }
}

class LinkedListTranlocal<E> extends BetaTranlocal {
    public LinkedListNode<E> head;
    public LinkedListNode<E> tail;

    public LinkedListTranlocal(LinkedList<E> owner) {
        super(owner);
    }

    @Override
    public void prepareForPooling(BetaObjectPool pool) {
        head = null;
        tail = null;
    }

    @Override
    public void openForRead(int desiredLockMode) {
        throw new TodoException();
    }

    @Override
    public void openForWrite(int desiredLockMode) {
        throw new TodoException();
    }

    //    @Override
//    public LinkedListTranlocal<E> openForWrite(BetaObjectPool pool) {
//        LinkedListTranlocal<E> tranlocal = new LinkedListTranlocal<E>((LinkedList<E>) owner);
//        //tranlocal.read = this;
//        tranlocal.head = head;
//        tranlocal.tail = tail;
//        return tranlocal;
//    }

    //@Override
    //public LinkedListTranlocal<E> openForCommute(BetaObjectPool pool) {
//        throw new TodoException();
    //  }

    @Override
    public boolean calculateIsDirty() {
        if (isReadonly()) {
            return false;
        }

        /*
        if (read == null) {
            isDirty = DIRTY_TRUE;
            return true;
        }

        LinkedListTranlocal<E> r = (LinkedListTranlocal<E>) read;
        if (r.head != head) {
            isDirty = DIRTY_TRUE;
            return true;
        }

        if (r.tail != tail) {
            isDirty = DIRTY_TRUE;
            return true;
        }

        isDirty = DIRTY_FALSE;
        return false;  */
        throw new TodoException();
    }


    @Override
    public void evaluateCommutingFunctions(BetaObjectPool pool) {
        throw new TodoException();
    }

    @Override
    public void addCommutingFunction(Function function, BetaObjectPool pool) {
        throw new TodoException();
    }
}

class LinkedListNode<E> extends AbstractBetaTransactionalObject {

    LinkedListNode(BetaTransaction tx) {
        super(tx);
    }

    @Override
    public Listeners ___commitAll(BetaTranlocal tranlocal, BetaTransaction tx, BetaObjectPool pool) {
        throw new TodoException();
    }

    @Override
    public BetaTranlocal ___newTranlocal() {
        throw new TodoException();
    }

    @Override
    public boolean ___load(int spinCount, BetaTransaction newLockOwner, int lockMode, BetaTranlocal tranlocal) {
        throw new TodoException();
    }

    @Override
    public Listeners ___commitDirty(BetaTranlocal tranlocal, BetaTransaction tx, BetaObjectPool pool) {
        throw new TodoException();
    }

    @Override
    public void ensure() {
        throw new TodoException();
    }

    @Override
    public void ensure(Transaction tx) {
        throw new TodoException();
    }

    @Override
    public void privatize() {
        throw new TodoException();
    }

    @Override
    public void privatize(Transaction tx) {
        throw new TodoException();
    }

    @Override
    public void deferredEnsure() {
        throw new TodoException();
    }

    @Override
    public void deferredEnsure(Transaction tx) {
        throw new TodoException();
    }

    @Override
    public String toDebugString() {
        throw new RuntimeException();
    }
}

class LinkedListNodeTranlocal<E> extends BetaTranlocal {

    LinkedListNode<E> next;
    LinkedListNode<E> prev;
    E value;

    LinkedListNodeTranlocal(BetaTransactionalObject owner) {
        super(owner);
    }

    @Override
    public void openForRead(int desiredLockMode) {
        throw new TodoException();
    }

    @Override
    public void openForWrite(int desiredLockMode) {
        throw new TodoException(                );
    }

    @Override
    public void prepareForPooling(BetaObjectPool pool) {
        next = null;
        prev = null;
        value = null;
    }

//    @Override
//    public LinkedListNodeTranlocal<E> openForWrite(BetaObjectPool pool) {
//        LinkedListNodeTranlocal<E> tranlocal = new LinkedListNodeTranlocal<E>(owner);
//        //tranlocal.read = this;
//        tranlocal.next = next;
//        tranlocal.prev = prev;
//        tranlocal.value = value;
//        return tranlocal;
//    }

//    @Override
//    public LinkedListNodeTranlocal<E> openForCommute(BetaObjectPool pool) {
//        throw new TodoException();
//    }

    @Override
    public boolean calculateIsDirty() {
        /*
        if (isCommitted) {
            return false;
        }

        if (read == null) {
            isDirty = DIRTY_TRUE;
            return true;
        }

        LinkedListNodeTranlocal<E> r = (LinkedListNodeTranlocal<E>) read;
        if (r.next != next) {
            isDirty = DIRTY_TRUE;
            return true;
        }

        if (r.prev != prev) {
            isDirty = DIRTY_TRUE;
            return true;
        }

        if (r.value != value) {
            isDirty = DIRTY_TRUE;
            return true;
        }

        isDirty = DIRTY_FALSE;
        return false;
        .*/
        throw new TodoException();
    }

    @Override
    public void evaluateCommutingFunctions(BetaObjectPool pool) {
        throw new TodoException();
    }

    @Override
    public void addCommutingFunction(Function function, BetaObjectPool pool) {
        throw new TodoException();
    }
}