package org.multiverse.api.collections;

import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;

public interface TransactionalMap<K, V> {

    Stm getStm();

    int size();

    int size(Transaction tx);

    boolean isEmpty();

    boolean isEmpty(Transaction tx);

    void clear();

    void clear(Transaction tx);

    V get(Object key);

    V get(Transaction tx, Object key);

    V put(K key, V value);

    V put(Transaction tx, K key, V value);

    V remove(Object key);

    V remove(Transaction tx, Object key);

    String toString(Transaction tx);
}
