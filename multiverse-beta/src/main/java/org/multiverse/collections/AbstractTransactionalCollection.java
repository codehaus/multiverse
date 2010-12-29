package org.multiverse.collections;

import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.collections.TransactionalCollection;
import org.multiverse.api.collections.TransactionalIterator;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.api.functions.Function;
import org.multiverse.api.references.RefFactory;

import java.util.Collection;

import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public abstract class AbstractTransactionalCollection<E> implements TransactionalCollection<E> {

    protected final Stm stm;
    protected final RefFactory defaultRefFactory;

    protected AbstractTransactionalCollection(Stm stm) {
        if (stm == null) {
            throw new NullPointerException();
        }
        this.stm = stm;
        this.defaultRefFactory = stm.getDefaultRefFactory();
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
    public boolean isEmpty(final Transaction tx) {
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
    public boolean contains(final Object item) {
        return contains(getThreadLocalTransaction(), item);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return containsAll(getThreadLocalTransaction(), c);
    }

    @Override
    public boolean containsAll(Transaction tx, Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        return remove(getThreadLocalTransaction(),o);
    }

    @Override
    public boolean remove(Transaction tx, Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(final E item) {
        return add(getThreadLocalTransaction(), item);
    }

    @Override
    public boolean addAll(final Collection<? extends E> c) {
        return addAll(getThreadLocalTransaction(), c);
    }

    @Override
    public boolean addAll(final Transaction tx, final Collection<? extends E> c) {
        if (c == null) {
            throw new NullPointerException();
        }

        if (c.isEmpty()) {
            return false;
        }

        boolean change = false;
        for (E item : c) {
            if (add(tx, item)) {
                change = true;
            }
        }

        return change;
    }

    @Override
    public boolean addAll(final TransactionalCollection<? extends E> c) {
        return addAll(getThreadLocalTransaction(), c);
    }

    @Override
    public boolean addAll(final Transaction tx, final TransactionalCollection<? extends E> c) {
        if (c == null) {
            throw new NullPointerException();
        }

        if (c.isEmpty(tx)) {
            return false;
        }

        boolean change = false;
        for (TransactionalIterator<? extends E> it = c.iterator(tx); it.hasNext(tx);) {

            if (add(tx, it.next(tx))) {
                change = true;
            }
        }

        return change;
    }

    @Override
    public TransactionalIterator<E> iterator() {
        return iterator(getThreadLocalTransaction());
    }

    @Override
    public TransactionalCollection<E> map(Function<E> function) {
        return map(getThreadLocalTransaction(),function);
    }

    @Override
    public TransactionalCollection<E> map(Transaction tx, Function<E> function) {
        throw new TodoException();
    }

    @Override
    public TransactionalCollection<E> filter() {
        return filter(getThreadLocalTransaction());
    }

    @Override
    public TransactionalCollection<E> filter(Transaction tx) {
        throw new TodoException();
    }

    @Override
    public TransactionalCollection<E> flatMap() {
        return flatMap(getThreadLocalTransaction());
    }

    @Override
    public TransactionalCollection<E> flatMap(Transaction tx) {
        throw new TodoException();
    }

    @Override
    public E foldLeft(Function<E> function) {
        return foldLeft(getThreadLocalTransaction(), function);
    }

    @Override
    public E foldLeft(Transaction tx, Function<E> function) {
        if(function == null){
            throw new NullPointerException("function can't be null");
        }

        throw new TodoException();
    }

    @Override
    public E foldRight(Function<E> function) {
        return foldRight(getThreadLocalTransaction(), function);
    }

    @Override
    public E foldRight(Transaction tx, Function<E> function) {
        throw new TodoException();
    }

    @Override
    public TransactionalCollection<E> foreach() {
        return foreach(getThreadLocalTransaction());
    }

    @Override
    public TransactionalCollection<E> foreach(Transaction tx) {
        throw new TodoException();
    }

    @Override
    public String toString() {
        return toString(getThreadLocalTransaction());
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }
}
