package org.multiverse.collections;

import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.collections.TransactionalMap;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.api.references.IntRef;

import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public final class NaiveTransactionalHashMap<K, V> implements TransactionalMap<K, V> {

    private final IntRef size;
    private final Stm stm;

    public NaiveTransactionalHashMap(Stm stm) {
        if (stm == null) {
            throw new NullPointerException();
        }
        this.stm = stm;
        this.size = stm.getDefaultRefFactory().newIntRef(0);
    }

    @Override
    public Stm getStm() {
        return stm;
    }

    @Override
    public void clear() {
        clear(getThreadLocalTransaction());
    }

    @Override
    public void clear(Transaction tx) {
        if (size.get(tx) == 0) {
            return;
        }

        throw new TodoException();
    }

    @Override
    public int size() {
        return size(getThreadLocalTransaction());
    }

    @Override
    public int size(Transaction tx) {
        return size.get(tx);
    }

    @Override
    public boolean isEmpty() {
        return isEmpty(getThreadLocalTransaction());
    }

    @Override
    public boolean isEmpty(Transaction tx) {
        return size.get(tx) == 0;
    }

    @Override
    public V get(Object key) {
        return get(getThreadLocalTransaction(), key);
    }

    @Override
    public V get(Transaction tx, Object key) {
        if (key == null) {
            throw new NullPointerException();
        }

        throw new TodoException();
    }

    @Override
    public V put(K key, V value) {
        return put(getThreadLocalTransaction(), key, value);
    }

    @Override
    public V put(Transaction tx, K key, V value) {
        throw new TodoException();
    }

    @Override
    public V remove(Object key) {
        return remove(getThreadLocalTransaction(), key);
    }

    @Override
    public V remove(Transaction tx, Object key) {
        throw new TodoException();
    }

    @Override
    public String toString(){
        return toString(getThreadLocalTransaction());
    }

    @Override
    public String toString(Transaction tx) {
        throw new TodoException();
    }
}
