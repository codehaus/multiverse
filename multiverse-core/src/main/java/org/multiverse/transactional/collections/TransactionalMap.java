package org.multiverse.transactional.collections;

import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * A Transactional version of the {@link ConcurrentMap}.
 *
 * @author Peter Veentjer.
 * @param <K> the key type for the map
 * @param <V> the value type for the map
 */
@TransactionalObject
public interface TransactionalMap<K, V> extends ConcurrentMap<K, V> {

    @Override
    @TransactionalMethod(readonly = true)
    int size();

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
}
