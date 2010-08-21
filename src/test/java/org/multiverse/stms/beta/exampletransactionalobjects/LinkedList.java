package org.multiverse.stms.beta.exampletransactionalobjects;

import org.multiverse.api.exceptions.TodoException;
import org.multiverse.functions.Function;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.transactionalobjects.AbstractBetaTransactionalObject;
import org.multiverse.stms.beta.transactionalobjects.BetaTransactionalObject;
import org.multiverse.stms.beta.transactionalobjects.Tranlocal;
import org.multiverse.stms.beta.transactions.BetaTransaction;

public class LinkedList<E> extends AbstractBetaTransactionalObject{

    public LinkedList(BetaTransaction tx) {
        super(tx);
    }

    public E removeFromFront(BetaTransaction tx, BetaObjectPool pool) {
        LinkedListTranlocal<E> tranlocal = (LinkedListTranlocal) tx.openForWrite(
                this, false, pool);
        if (tranlocal.head == null) {
            return null;
        }

        if (tranlocal.head == tranlocal.tail) {
            LinkedListNodeTranlocal<E> nodeTranlocal = (LinkedListNodeTranlocal<E>) tx.openForRead(
                    tranlocal.head, false, pool);
            tranlocal.head = null;
            tranlocal.tail = null;
            return nodeTranlocal.value;
        } else {
            LinkedListNodeTranlocal<E> headTranlocal = (LinkedListNodeTranlocal<E>)tx.openForWrite(
                    tranlocal.head, false, pool);

            LinkedListNodeTranlocal<E> nextHeadTranlocal = (LinkedListNodeTranlocal<E>)tx.openForWrite(
                    headTranlocal.next, false, pool);

            nextHeadTranlocal.prev = null;
            tranlocal.head = (LinkedListNode<E>) nextHeadTranlocal.owner;
            return headTranlocal.value;
        }
    }

    public E removeFromBack(BetaTransaction tx, BetaObjectPool pool) {
        LinkedListTranlocal tranlocal = (LinkedListTranlocal) tx.openForWrite(this, false, pool);
        if (tranlocal.head == null) {
            return null;
        }

        if (tranlocal.head == tranlocal.tail) {
            LinkedListNodeTranlocal<E> nodeTranlocal = (LinkedListNodeTranlocal<E>) tx.openForRead(
                    tranlocal.head, false, pool);
            tranlocal.head = null;
            tranlocal.tail = null;
            return nodeTranlocal.value;
        } else {
            LinkedListNodeTranlocal<E> tailTranlocal = (LinkedListNodeTranlocal<E>)tx.openForWrite(
                    tranlocal.tail, false, pool);

            LinkedListNodeTranlocal<E> prevTailTranlocal = (LinkedListNodeTranlocal<E>)tx.openForWrite(
                    tailTranlocal.prev, false, pool);

            prevTailTranlocal.next = null;
            tranlocal.tail = (LinkedListNode) prevTailTranlocal.owner;
            return tailTranlocal.value;
        }
    }

    public void addInFront(BetaTransaction tx, BetaObjectPool pool, E item) {
        LinkedListTranlocal tranlocal = (LinkedListTranlocal) tx.openForWrite(this, false, pool);
        LinkedListNode newNode = new LinkedListNode(tx);
        LinkedListNodeTranlocal newNodeTranlocal = (LinkedListNodeTranlocal) tx.openForConstruction(newNode, pool);
        newNodeTranlocal.value = item;

        if (tranlocal.head == null) {
            tranlocal.head = newNode;
            tranlocal.tail = newNode;
        } else {
             LinkedListNodeTranlocal<E> headTranlocal = (LinkedListNodeTranlocal) tx.openForWrite(
                    tranlocal.head, false, pool);

            headTranlocal.prev = newNode;
            newNodeTranlocal.next = tranlocal.head;
            tranlocal.head = newNode;
        }
    }

    public void addInBack(BetaTransaction tx, BetaObjectPool pool, E item) {
        LinkedListTranlocal tranlocal = (LinkedListTranlocal) tx.openForWrite(this, false, pool);
        LinkedListNode newNode = new LinkedListNode(tx);

        LinkedListNodeTranlocal<E> newNodeTranlocal = (LinkedListNodeTranlocal) tx.openForConstruction(newNode, pool);
        newNodeTranlocal.value = item;

        if (tranlocal.head == null) {
            tranlocal.head = newNode;
            tranlocal.tail = newNode;
        } else {
            LinkedListNodeTranlocal<E> tailTranlocal = (LinkedListNodeTranlocal) tx.openForWrite(
                    tranlocal.tail, false, pool);

            tailTranlocal.next = newNode;
            newNodeTranlocal.prev = tranlocal.tail;
            tranlocal.tail = newNode;
        }
    }

    @Override
    public LinkedListTranlocal ___openForConstruction(BetaObjectPool pool) {
        return new LinkedListTranlocal<E>(this);
    }

    @Override
    public LinkedListTranlocal ___openForCommute(BetaObjectPool pool) {
        LinkedListTranlocal tranlocal = new LinkedListTranlocal<E>(this);
        tranlocal.isCommuting = true;
        return tranlocal;
    }
}

class LinkedListTranlocal<E> extends Tranlocal {
    public LinkedListNode<E> head;
    public LinkedListNode<E> tail;

    public LinkedListTranlocal(LinkedList<E> owner) {
        super(owner, false);
    }

    @Override
    public void prepareForPooling(BetaObjectPool pool) {
        head = null;
        tail = null;
    }

    @Override
    public LinkedListTranlocal<E> openForWrite(BetaObjectPool pool) {
        LinkedListTranlocal<E> tranlocal = new LinkedListTranlocal<E>((LinkedList<E>) owner);
        tranlocal.read = this;
        tranlocal.head = head;
        tranlocal.tail = tail;
        return tranlocal;
    }

    @Override
    public LinkedListTranlocal<E> openForCommute(BetaObjectPool pool) {
        throw new TodoException();
    }

    @Override
    public boolean calculateIsDirty() {
        if (isCommitted) {
            return false;
        }

        if (read == null) {
            isDirty = true;
            return true;
        }

        LinkedListTranlocal<E> r = (LinkedListTranlocal<E>) read;
        if (r.head != head) {
            isDirty = true;
            return true;
        }

        if (r.tail != tail) {
            isDirty = true;
            return true;
        }

        return false;
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

class LinkedListNode<E> extends AbstractBetaTransactionalObject{

    LinkedListNode(BetaTransaction tx) {
        super(tx);
    }

    @Override
    public LinkedListNodeTranlocal<E> ___openForConstruction(BetaObjectPool pool) {
        return new LinkedListNodeTranlocal<E>(this);
    }

    @Override
    public LinkedListNodeTranlocal<E> ___openForCommute(BetaObjectPool pool) {
        LinkedListNodeTranlocal<E> tranlocal = new LinkedListNodeTranlocal<E>(this);
        tranlocal.isCommuting = true;
        return tranlocal;
    }
}

class LinkedListNodeTranlocal<E> extends Tranlocal {

    LinkedListNode<E> next;
    LinkedListNode<E> prev;
    E value;

    LinkedListNodeTranlocal(BetaTransactionalObject owner) {
        super(owner, false);
    }

    @Override
    public void prepareForPooling(BetaObjectPool pool) {
        next = null;
        prev = null;
        value = null;
    }

    @Override
    public LinkedListNodeTranlocal<E> openForWrite(BetaObjectPool pool) {
        LinkedListNodeTranlocal<E> tranlocal = new LinkedListNodeTranlocal<E>(owner);
        tranlocal.read = this;
        tranlocal.next = next;
        tranlocal.prev = prev;
        tranlocal.value = value;
        return tranlocal;
    }

    @Override
    public LinkedListNodeTranlocal<E> openForCommute(BetaObjectPool pool) {
        throw new TodoException();
    }

    @Override
    public boolean calculateIsDirty() {
        if (isCommitted) {
            return false;
        }

        if (read == null) {
            isDirty = true;
            return true;
        }

        LinkedListNodeTranlocal<E> r = (LinkedListNodeTranlocal<E>) read;
        if (r.next != next) {
            isDirty = true;
            return true;
        }

        if (r.prev != prev) {
            isDirty = true;
            return true;
        }

        if (r.value != value) {
            isDirty = true;
            return true;
        }

        return false;
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