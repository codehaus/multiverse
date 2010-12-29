package org.multiverse.api.collections;

import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;

import java.util.Map;

public interface TransactionalMap<K, V> extends Map<K, V> {

    Stm getStm();

    int size(Transaction tx);

    boolean isEmpty(Transaction tx);

    void clear(Transaction tx);

    V get(Transaction tx, Object key);

    boolean containsKey(Transaction tx, Object key);

    boolean containsValue(Transaction tx, Object value);

    V put(Transaction tx, K key, V value);

    void putAll(Transaction tx, Map<? extends K, ? extends V> m);

    V remove(Transaction tx, Object key);

    TransactionalCollection<V> values();

    TransactionalCollection<V> values(Transaction tx);

    TransactionalSet<K> keySet();

    TransactionalSet<K> keySet(Transaction tx);

    TransactionalSet<Entry<K, V>> entrySet();

    TransactionalSet<Entry<K, V>> entrySet(Transaction tx);

    String toString(Transaction tx);
}
