package org.multiverse.transactional.collections;

import org.multiverse.annotations.Exclude;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.api.programmatic.ProgrammaticLong;
import org.multiverse.api.programmatic.ProgrammaticReferenceFactory;
import org.multiverse.utils.TodoException;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import static java.lang.Math.max;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

/**
 * A Tree based TransactionalMap implementation. Essentially is the transactional version of the
 * {@link java.util.TreeMap}.
 *
 * @author Peter Veentjer
 * @see org.multiverse.transactional.collections.TransactionalMap
 * @see java.util.concurrent.ConcurrentMap
 * @see java.util.Map
 */
public final class TransactionalTreeMap<K, V> implements TransactionalMap<K, V> {

    private final static ProgrammaticReferenceFactory sizeFactory = getGlobalStmInstance()
            .getProgrammaticReferenceFactoryBuilder()
            .build();


    private final Comparator<? super K> comparator;

    private final ProgrammaticLong size = sizeFactory.atomicCreateLong(0);

    private Node<K, V> root;

    public TransactionalTreeMap() {
        this.comparator = null;
    }

    public TransactionalTreeMap(Comparator<? super K> comparator) {
        this.comparator = comparator;
    }

    /**
     * Returns the Comparator uses by this TransactionalTreeMap to do comparisons between keys.
     * If the keys
     *
     * @return the Comparator used.
     */
    public final Comparator getComparator() {
        return comparator;
    }

    /**
     * Returns the height of this TransactionalTreeMap. The height is the maximum number of nodes from
     * the rootnode to a leaf node. An empty tree will return 0.
     *
     * @return the height of this TransactionalTreeMap.
     */
    @TransactionalMethod(readonly = true)
    public int height() {
        //todo: at the moment this method is recursive, but should be converted to iterative.
        return height(root);
    }

    private int height(Node<K, V> node) {
        if (node == null) {
            return 0;
        }

        int left = height(node.left);
        int right = height(node.right);
        return 1 + max(left, right);
    }

    @Override
    public void clear() {
        //prevent doing an open for write.
        if (root == null) {
            return;
        }

        size.set(0);
        root = null;
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
        return (int) size.get();
    }

    @Override
    @Exclude
    public int getCurrentSize() {
        return (int) size.atomicGet();
    }

    @Override
    public boolean isEmpty() {
        return root == null;
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
    public V put(K key, V value) {
        if (key == null) {
            throw new NullPointerException();
        }

        if (root == null) {
            size.commutingInc(1);
            root = new Node<K, V>(key, value);
            return null;
        }

        //todo: no balancing is done yet.
        Node<K, V> node = root;
        while (true) {
            int cmp = compareTo(key, node.key);
            if (cmp < 0) {
                if (node.left != null) {
                    node = node.left;
                } else {
                    Node<K, V> newNode = new Node<K, V>(key, value);
                    newNode.parent = node;
                    node.left = newNode;
                    size.commutingInc(1);
                    return null;
                }
            } else if (cmp > 0) {
                if (node.right != null) {
                    node = node.right;
                } else {
                    Node<K, V> newNode = new Node<K, V>(key, value);
                    newNode.parent = node;
                    node.right = newNode;
                    size.commutingInc(1);
                    return null;
                }
            } else {
                V oldValue = node.value;
                node.value = value;
                return oldValue;
            }
        }
    }

    private int compareTo(K key1, K key2) {
        if (comparator != null) {
            return comparator.compare(key1, key2);
        } else {
            Comparable<? super K> k = (Comparable<? super K>) key1;
            return k.compareTo(key2);
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        if (m == null) {
            throw new NullPointerException();
        }

        if (m.isEmpty()) {
            return;
        }

        for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public V replace(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException();
        }

        Node<K, V> node = findNode(key);
        if (node == null) {
            return null;
        }

        V oldValue = node.value;
        node.value = value;
        return oldValue;
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
        if (key == null) {
            throw new NullPointerException();
        }

        Node<K, V> node = findNode(key);
        if (node == null) {
            return null;
        }

        throw new TodoException();
    }

    @Override
    public V putIfAbsent(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException();
        }

        Node<K, V> node = findNode(key);
        if (node == null) {

        }

        throw new TodoException();
    }

    @Override
    public boolean remove(Object key, Object value) {
        if (key == null || value == null) {
            throw new NullPointerException();
        }

        throw new TodoException();
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
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
        final K key;

        V value;
        Node<K, V> parent;
        Node<K, V> left;
        Node<K, V> right;
        int height;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }

        void rotateLeft() {

        }

        void rotateRight() {

        }

        void doubleRotateLeft() {

        }

        void doubleRotateRight() {

        }
    }
}