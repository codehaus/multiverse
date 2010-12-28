package org.multiverse.api.collections;

import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;

import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class NaiveHashSet<E> implements TransactionalSet<E> {

    private final Stm stm;
    private final NaiveTransactionalHashMap<E, Object> map;

    public NaiveHashSet(Stm stm) {
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
}
