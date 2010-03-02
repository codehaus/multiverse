package org.multiverse.transactional.collections;

import org.multiverse.annotations.TransactionalObject;
import org.multiverse.utils.TodoException;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

/**
 * A Tree based TransactionalMap implementation.
 *
 * @author Peter Veentjer
 */
public class TransactionalTreeMap<K, V> implements TransactionalMap<K, V> {

    private final Comparator<? super K> comparator;

    private int size;
    private Node<K, V> root;

    public TransactionalTreeMap() {
        this.comparator = null;
    }

    public TransactionalTreeMap(Comparator<? super K> comparator) {
        this.comparator = comparator;
    }

    public final Comparator getComparator() {
        return comparator;
    }

    @Override
    public boolean containsKey(Object key) {
        if (key == null) {
            throw new NullPointerException();
        }

        return findNode(key) != null;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return root == null;
    }

    @Override
    public V put(K key, V value) {
        if (key == null) {
            throw new NullPointerException();
        }

        if (root == null) {
            size++;
            root = new Node<K, V>(key, value);
            return null;
        } else {
            throw new TodoException();
        }
    }

    @Override
    public V get(Object key) {
        if (key == null) {
            throw new NullPointerException();
        }

        Node<K, V> node = findNode(key);
        return node == null ? null : node.value;
    }

    @Override
    public boolean containsValue(Object value) {
        throw new TodoException();
    }

    @Override
    public Set<K> keySet() {
        throw new TodoException();
    }

    @Override
    public Collection<V> values() {
        throw new TodoException();
    }

    @Override
    public V remove(Object key) {
        throw new TodoException();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new TodoException();
    }

    @Override
    public void clear() {
        size = 0;
        root = null;
    }

    @Override
    public V putIfAbsent(K key, V value) {
        throw new TodoException();
    }

    @Override
    public boolean remove(Object key, Object value) {
        throw new TodoException();
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        throw new TodoException();
    }

    @Override
    public V replace(K key, V value) {
        throw new TodoException();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new TodoException();
    }

    @Override
    public String toString() {
        throw new TodoException();
    }

    @Override
    public int hashCode() {
        throw new TodoException();
    }

    @Override
    public boolean equals(Object thatObject) {
        if (thatObject == this) {
            return true;
        }

        throw new TodoException();
    }

    /**
     * Returns this map's entry for the given key, or <tt>null</tt> if the map
     * does not contain an entry for the key.
     *
     * @return this map's entry for the given key, or <tt>null</tt> if the map
     *         does not contain an entry for the key
     * @throws ClassCastException   if the specified key cannot be compared
     *                              with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *                              and this map uses natural ordering, or its comparator
     *                              does not permit null keys
     */
    final Node<K, V> findNode(Object key) {
        // Offload comparator-based version for sake of performance
        if (comparator != null) {
            return findNodeUsingComparator(key);
        }

        if (key == null) {
            throw new NullPointerException();
        }

        Comparable<? super K> k = (Comparable<? super K>) key;
        Node<K, V> p = root;
        while (p != null) {
            int cmp = k.compareTo(p.key);
            if (cmp < 0) {
                p = p.left;
            } else if (cmp > 0) {
                p = p.right;
            } else {
                return p;
            }
        }
        return null;
    }

    /**
     * Version of findNode using comparator. Split off from findNode
     * for performance. (This is not worth doing for most methods,
     * that are less dependent on comparator performance, but is
     * worthwhile here.)
     */
    final Node<K, V> findNodeUsingComparator(Object key) {
        K k = (K) key;

        if (comparator != null) {
            Node<K, V> p = root;
            while (p != null) {
                int cmp = comparator.compare(k, p.key);
                if (cmp < 0) {
                    p = p.left;
                } else if (cmp > 0) {
                    p = p.right;
                } else {
                    return p;
                }
            }
        }
        return null;
    }

    @TransactionalObject
    static class Node<K, V> {
        K key;
        V value;
        Node<K, V> left;
        Node<K, V> right;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }


}
