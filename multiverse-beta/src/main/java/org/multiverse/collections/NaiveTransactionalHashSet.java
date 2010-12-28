package org.multiverse.collections;

import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.collections.TransactionalIterator;
import org.multiverse.api.collections.TransactionalSet;
import org.multiverse.api.exceptions.TodoException;

import java.util.Map;

import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public final class NaiveTransactionalHashSet<E> implements TransactionalSet<E> {

    private final Stm stm;
    private final NaiveTransactionalHashMap<E, Object> map;

    public NaiveTransactionalHashSet(Stm stm) {
        if (stm == null) {
            throw new NullPointerException();
        }
        this.stm = stm;
        this.map = new NaiveTransactionalHashMap<E, Object>(stm);
    }

    @Override
    public Stm getStm() {
        return stm;
    }

    @Override
    public boolean add(E item) {
        return add(getThreadLocalTransaction(), item);
    }

    @Override
    public boolean add(Transaction tx, E item) {
        return map.put(tx, item, this) == null;
    }

    @Override
    public boolean contains(Object item) {
        return contains(getThreadLocalTransaction(), item);
    }

    @Override
    public boolean contains(Transaction tx, Object item) {
        return map.get(tx, item) != null;
    }

    @Override
    public boolean remove(Object item) {
        return remove(getThreadLocalTransaction(), item);
    }

    @Override
    public boolean remove(Transaction tx, Object item) {
        return map.remove(tx, item) != null;
    }

    @Override
    public int size() {
        return size(getThreadLocalTransaction());
    }

    @Override
    public int size(Transaction tx) {
        return map.size(tx);
    }

    @Override
    public boolean isEmpty() {
        return isEmpty(getThreadLocalTransaction());
    }

    @Override
    public boolean isEmpty(Transaction tx) {
        return map.isEmpty(tx);
    }

    @Override
    public void clear() {
        clear(getThreadLocalTransaction());
    }

    @Override
    public void clear(Transaction tx) {
        map.clear(tx);
    }

    @Override
    public String toString() {
        return toString(getThreadLocalTransaction());
    }

    @Override
    public TransactionalIterator<E> iterator() {
        return iterator(getThreadLocalTransaction());
    }

    @Override
    public TransactionalIterator<E> iterator(Transaction tx) {
        return map.keySet(tx).iterator(tx);
    }

    static class It<E> implements TransactionalIterator<E> {

        private final TransactionalIterator<Map.Entry<E, Object>> iterator;

        It(TransactionalIterator<Map.Entry<E, Object>> iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            return hasNext(getThreadLocalTransaction());
        }

        @Override
        public boolean hasNext(Transaction tx) {
            return iterator.hasNext(tx);
        }

        @Override
        public E next() {
            return next(getThreadLocalTransaction());
        }
        @Override
        public E next(Transaction tx) {
            return iterator.next(tx).getKey();
        }

        @Override
        public void remove() {
            remove(getThreadLocalTransaction());
        }

        @Override
        public void remove(Transaction tx) {
            iterator.remove(tx);
        }
    }

    @Override
    public String toString(Transaction tx) {
        throw new TodoException();
    }
}
