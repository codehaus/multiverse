package org.multiverse.stms.alpha.manualinstrumentation;

import static org.multiverse.api.StmUtils.retry;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTransaction;
import org.multiverse.stms.alpha.manualinstrumentation.IntStackTranlocal.IntNode;
import org.multiverse.stms.alpha.mixins.FastAtomicObjectMixin;
import org.multiverse.templates.AtomicTemplate;

public final class IntStack extends FastAtomicObjectMixin {

    public IntStack() {
        new AtomicTemplate() {
            @Override
            public Object execute(Transaction t) {
                IntStackTranlocal tranlocal = (IntStackTranlocal) ((AlphaTransaction) t).load(IntStack.this);
                return null;
            }
        }.execute();
    }

    public int size() {
        return new AtomicTemplate<Integer>(true) {
            @Override
            public Integer execute(Transaction t) {
                IntStackTranlocal tranlocal = (IntStackTranlocal) ((AlphaTransaction) t).load(IntStack.this);
                return size(tranlocal);
            }
        }.execute();
    }

    public boolean isEmpty() {
        return new AtomicTemplate<Boolean>(true) {
            @Override
            public Boolean execute(Transaction t) {
                IntStackTranlocal tranlocal = (IntStackTranlocal) ((AlphaTransaction) t).load(IntStack.this);
                return isEmpty(tranlocal);
            }
        }.execute();
    }

    public int pop() {
        return new AtomicTemplate<Integer>() {
            @Override
            public Integer execute(Transaction t) {
                IntStackTranlocal tranlocal = (IntStackTranlocal) ((AlphaTransaction) t).load(IntStack.this);
                return pop(tranlocal);
            }
        }.execute();
    }

    public int pop(Transaction t) {
        IntStackTranlocal tranlocal = (IntStackTranlocal) ((AlphaTransaction) t).load(IntStack.this);
        return pop(tranlocal);
    }

    public void push(final int value) {
        new AtomicTemplate() {
            @Override
            public Integer execute(Transaction t) {
                IntStackTranlocal tranlocal = (IntStackTranlocal) ((AlphaTransaction) t).load(IntStack.this);
                push(tranlocal, value);
                return null;
            }
        }.execute();
    }

    public void push(Transaction t, int value) {
        IntStackTranlocal tranlocal = (IntStackTranlocal) ((AlphaTransaction) t).load(this);
        push(tranlocal, value);
    }

    @Override
    public AlphaTranlocal ___loadUpdatable(long version) {
        IntStackTranlocal origin = (IntStackTranlocal) ___load(version);
        if (origin == null) {
            return new IntStackTranlocal(this);
        } else {
            return new IntStackTranlocal(origin);
        }
    }

    public int size(IntStackTranlocal tranlocal) {
        return tranlocal.size;
    }

    public boolean isEmpty(IntStackTranlocal tranlocal) {
        return tranlocal.size == 0;
    }

    public void push(IntStackTranlocal tranlocal, int value) {
        if (tranlocal.___writeVersion > 0) {
            throw new ReadonlyException();
        }

        tranlocal.head = new IntNode(value, tranlocal.head);
        tranlocal.size++;
    }

    public int pop(IntStackTranlocal tranlocal) {
        if (tranlocal.___writeVersion > 0) {
            throw new ReadonlyException();
        }

        if (tranlocal.head == null) {
            retry();
        }

        tranlocal.size--;
        IntNode oldHead = tranlocal.head;
        tranlocal.head = oldHead.next;
        return oldHead.value;
    }
}

