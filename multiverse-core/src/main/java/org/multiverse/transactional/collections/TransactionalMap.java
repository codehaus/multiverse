package org.multiverse.transactional.collections;

import org.multiverse.annotations.Exclude;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * A Transactional version of the {@link ConcurrentMap}.
 *
 * @author Peter Veentjer.
 * @param <K> the key type for the map
 * @param <V> the value type for the map
 * @see java.util.concurrent.ConcurrentMap
 * @see java.util.Map
 */
@TransactionalObject
public interface TransactionalMap<K, V> extends ConcurrentMap<K, V> {

    @Override
    @TransactionalMethod(readonly = true)
    int size();

    /**
     * Returns the current size of the TransactionalMap. The big difference between
     * the normal {@link #size()} method is that this one returns the actual size of
     * this map and doesn't look at the current transaction. So you could see changes
     * made by other threads.
     *
     * @return the current size of the TransactionalMap.
     */
    @Exclude
    int getCurrentSize();

    @Override
    @TransactionalMethod(readonly = true)
    boolean isEmpty();

    @Override
    @TransactionalMethod(readonly = true)
    boolean containsKey(Object key);

    @Override
    @TransactionalMethod(readonly = true)
    boolean containsValue(Object value);

    @Override
    @TransactionalMethod(readonly = true)
    V get(Object key);

    @Override
    @TransactionalMethod(readonly = true)
    Set<K> keySet();

    @Override
    @TransactionalMethod(readonly = true)
    Collection<V> values();

    @Override
    @TransactionalMethod(readonly = true)
    Set<Entry<K, V>> entrySet();

    @Override
    @TransactionalMethod(readonly = true)
    String toString();

    @Override
    @TransactionalMethod(readonly = true)
    boolean equals(Object o);

    @Override
    @TransactionalMethod(readonly = true)
    int hashCode();

    @Override
    V put(K key, V value);

    @Override
    V remove(Object key);

    @Override
    void putAll(Map<? extends K, ? extends V> m);

    @Override
    void clear();

    @Override
    V putIfAbsent(K key, V value);

    @Override
    boolean remove(Object key, Object value);

    @Override
    boolean replace(K key, V oldValue, V newValue);

    @Override
    V replace(K key, V value);
}
