package org.multiverse.collections;

import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.collections.TransactionalCollection;
import org.multiverse.api.collections.TransactionalIterator;
import org.multiverse.api.exceptions.TodoException;

import java.util.Collection;

import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public abstract class AbstractTransactionalCollection<E> implements TransactionalCollection<E> {

    protected final Stm stm;

    protected AbstractTransactionalCollection(Stm stm) {
        if (stm == null) {
            throw new NullPointerException();
        }
        this.stm = stm;
    }

    @Override
    public Stm getStm() {
        return stm;
    }

    @Override
    public boolean isEmpty() {
        return isEmpty(getThreadLocalTransaction());
    }

    @Override
    public boolean isEmpty(Transaction tx) {
        return size(tx) == 0;
    }

    @Override
    public int size() {
        return size(getThreadLocalTransaction());
    }

    @Override
    public void clear() {
        clear(getThreadLocalTransaction());
    }

    @Override
    public boolean contains(Object item) {
        return contains(getThreadLocalTransaction(), item);
    }

    @Override
    public boolean add(E item) {
        return add(getThreadLocalTransaction(), item);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return addAll(getThreadLocalTransaction(), c);
    }

    @Override
    public boolean addAll(Transaction tx, Collection<? extends E> c) {
       throw new TodoException();
    }

    @Override
    public boolean addAll(TransactionalCollection<? extends E> c) {
        return addAll(getThreadLocalTransaction(),c);
    }

    @Override
    public boolean addAll(Transaction tx, TransactionalCollection<? extends E> c) {
        throw new TodoException();
    }

    @Override
    public TransactionalIterator<E> iterator() {
        return iterator(getThreadLocalTransaction());
    }

    @Override
    public String toString() {
        return toString(getThreadLocalTransaction());
    }
}
