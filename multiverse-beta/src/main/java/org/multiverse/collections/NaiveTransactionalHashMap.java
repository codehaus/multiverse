package org.multiverse.collections;

import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.collections.TransactionalCollection;
import org.multiverse.api.collections.TransactionalSet;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.api.references.IntRef;

public final class NaiveTransactionalHashMap<K, V> extends AbstractTransactionalMap<K, V> {

    private final IntRef size;

    public NaiveTransactionalHashMap(Stm stm) {
        super(stm);
        this.size = stm.getDefaultRefFactory().newIntRef(0);
    }

    @Override
    public void clear(Transaction tx) {
        if (size.get(tx) == 0) {
            return;
        }

        throw new TodoException();
    }

    @Override
    public int size(Transaction tx) {
        return size.get(tx);
    }

    @Override
    public V get(Transaction tx, Object key) {
        if (key == null) {
            throw new NullPointerException();
        }

        throw new TodoException();
    }

    @Override
    public V put(Transaction tx, K key, V value) {
        if(key == null){
            throw new NullPointerException();
        }

        throw new TodoException();
    }

    @Override
    public V remove(Transaction tx, Object key) {
        throw new TodoException();
    }

    @Override
    public String toString(Transaction tx) {
        int s = size.get(tx);
        if(s == 0){
            return "[]";
        }

        throw new TodoException();
    }

    @Override
    public TransactionalSet<Entry<K, V>> entrySet(Transaction tx) {
        throw new TodoException();
    }

    @Override
    public TransactionalSet<K> keySet(Transaction tx) {
        throw new TodoException();
    }

    @Override
    public boolean containsKey(Transaction tx, Object key) {
        throw new TodoException();
    }

    @Override
    public boolean containsValue(Transaction tx, Object value) {
        throw new TodoException();
    }

    @Override
    public TransactionalCollection<V> values(Transaction tx) {
        throw new TodoException();
    }
}
