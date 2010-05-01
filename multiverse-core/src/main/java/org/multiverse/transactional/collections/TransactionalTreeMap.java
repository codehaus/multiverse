package org.multiverse.transactional.collections;

import org.multiverse.annotations.NonTransactional;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.api.programmatic.ProgrammaticLong;
import org.multiverse.api.programmatic.ProgrammaticReferenceFactory;
import org.multiverse.utils.TodoException;

import java.util.*;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

/**
 * A Tree based TransactionalMap implementation. Essentially is the transactional version of the
 * {@link java.util.TreeMap}.
 * <p/>
 * A lot of logic from the AbstractMap is copied in this class. This is done to make sure that
 * also transactional versions of the method are available. This is useful for bytecode optimizations,
 * but it also is important for adding the transactions (transactions are not added to the
 * AbstractMap).
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

    private EntryImpl<K, V> root;

    /**
     * Creates a new TransactionalTreeMap.
     */
    public TransactionalTreeMap() {
        this.comparator = null;
    }

    /**
     * Creates a new TransactionalTreeMap with the provided comparator.
     *
     * @param comparator the Comparator used to compare keys (is allowed to be null meaning
     *                   that the  the value needs to be Sorable.
     */
    public TransactionalTreeMap(Comparator<? super K> comparator) {
        this.comparator = comparator;
    }

    /**
     * Creates a new TransactionalTreeMap
     *
     * @param initial the initial content of this TransactionalTreeMap.
     * @throws NullPointerException if initial is null.
     */
    public TransactionalTreeMap(Map<K, V> initial) {
        this();

        if (initial == null) {
            throw new NullPointerException();
        }

        for (Map.Entry<K, V> entries : initial.entrySet()) {
            put(entries.getKey(), entries.getValue());
        }
    }

    /**
     * Returns the Comparator uses by this TransactionalTreeMap to do comparisons between keys.
     * If the keys
     *
     * @return the Comparator used.
     */
    @NonTransactional
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
        return root.height();
    }

    @Override
    public void clear() {
        //prevent making anything dirty if not needed.
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

        return getEntry(key) != null;
    }

    @Override
    public int size() {
        return (int) size.get();
    }

    @Override
    @NonTransactional
    public int atomicSize() {
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

        EntryImpl<K, V> node = getEntry(key);
        return node == null ? null : node.value;
    }

    @Override
    public V put(K key, V value) {
        V oldValue = insert(key, value);
        if (!isBalanced()) {
            root = balance(root);
        }
        return oldValue;
    }

    private V insert(K key, V value) {
        if (key == null) {
            throw new NullPointerException();
        }

        if (root == null) {
            size.commutingInc(1);
            root = new EntryImpl<K, V>(key, value);
            return null;
        }

        EntryImpl<K, V> node = root;
        while (true) {
            int cmp = compareTo(key, node.key);
            if (cmp < 0) {
                if (node.left != null) {
                    node = node.left;
                } else {
                    EntryImpl<K, V> newNode = new EntryImpl<K, V>(key, value);
                    newNode.parent = node;
                    node.left = newNode;
                    size.commutingInc(1);
                    return null;
                }
            } else if (cmp > 0) {
                if (node.right != null) {
                    node = node.right;
                } else {
                    EntryImpl<K, V> newNode = new EntryImpl<K, V>(key, value);
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

    private EntryImpl<K, V> balance(EntryImpl<K, V> node) {
        if (node.isRightHeavy()) {
            if (node.right.isLeftHeavy()) {
                node = node.doubleRotateLeft();
            } else {
                node = node.rotateLeft();
            }
        }
        if (node.isLeftHeavy()) {
            if (node.left.isRightHeavy()) {
                node = node.doubleRotateRight();
            } else {
                node = node.rotateRight();
            }
        }
        balanceChildrenIfNecessary(node);
        return node;
    }

    private void balanceChildrenIfNecessary(EntryImpl<K, V> node) {
        if (!isBalanced(node.right)) {
            node.right = balance(node.right);
        }
        if (!isBalanced(node.left)) {
            node.left = balance(node.left);
        }
    }

    private boolean isBalanced(EntryImpl<K, V> node) {
        return node.balanceFactor() < 2;
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

        EntryImpl<K, V> node = getEntry(key);
        if (node == null) {
            return null;
        }

        V oldValue = node.value;
        node.value = value;
        return oldValue;
    }

    @Override
    public boolean containsValue(Object value) {
        Iterator<Entry<K, V>> i = entrySet().iterator();
        if (value == null) {
            while (i.hasNext()) {
                Entry<K, V> e = i.next();
                if (e.getValue() == null) {
                    return true;
                }
            }
        } else {
            while (i.hasNext()) {
                Entry<K, V> e = i.next();
                if (value.equals(e.getValue())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Set<K> keySet() {
        return new AbstractSet<K>() {
            public Iterator<K> iterator() {
                return new Iterator<K>() {
                    private Iterator<Entry<K, V>> i = entrySet().iterator();

                    public boolean hasNext() {
                        return i.hasNext();
                    }

                    public K next() {
                        return i.next().getKey();
                    }

                    public void remove() {
                        i.remove();
                    }
                };
            }

            public int size() {
                return TransactionalTreeMap.this.size();
            }

            public boolean contains(Object k) {
                return TransactionalTreeMap.this.containsKey(k);
            }
        };
    }

    @Override
    public Collection<V> values() {
        return new AbstractCollection<V>() {
            public Iterator<V> iterator() {
                return new Iterator<V>() {
                    private Iterator<Entry<K, V>> i = entrySet().iterator();

                    public boolean hasNext() {
                        return i.hasNext();
                    }

                    public V next() {
                        return i.next().getValue();
                    }

                    public void remove() {
                        i.remove();
                    }
                };
            }

            public int size() {
                return TransactionalTreeMap.this.size();
            }

            public boolean contains(Object v) {
                return TransactionalTreeMap.this.containsValue(v);
            }
        };
    }

    @Override
    public V remove(Object key) {
        if (key == null) {
            throw new NullPointerException();
        }

        EntryImpl<K, V> p = getEntry(key);
        if (p == null) {
            return null;
        }

        V oldValue = p.value;
        deleteEntry(p);
        return oldValue;
    }

    private void deleteEntry(EntryImpl<K, V> p) {
        throw new TodoException();
    }

    @Override
    public V putIfAbsent(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException();
        }

        EntryImpl<K, V> node = getEntry(key);
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
        return new EntrySet();
    }


    @Override
    public String toString() {
        if (isEmpty()) {
            return "{}";
        }

        Iterator<Entry<K, V>> i = entrySet().iterator();
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        for (; ;) {
            Entry<K, V> e = i.next();
            K key = e.getKey();
            V value = e.getValue();
            sb.append(key == this ? "(this Map)" : key);
            sb.append('=');
            sb.append(value == this ? "(this Map)" : value);
            if (!i.hasNext()) {
                return sb.append('}').toString();
            }
            sb.append(", ");
        }
    }

    @Override
    public int hashCode() {
        int result = 0;

        if (isEmpty()) {
            return result;
        }

        for (Entry<K, V> kvEntry : entrySet()) {
            result += kvEntry.hashCode();
        }
        return result;
    }

    @Override
    public boolean equals(Object thatObject) {
        if (thatObject == this) {
            return true;
        }

        if (!(thatObject instanceof Map)) {
            return false;
        }

        Map<K, V> that = (Map<K, V>) thatObject;
        if (that.size() != this.size()) {
            return false;
        }

        for (Map.Entry<K, V> entry : entrySet()) {
            V thisValue = entry.getValue();
            V thatValue = that.get(entry.getKey());
            if (!valEquals(thisValue, thatValue)) {
                return false;
            }
        }

        return true;
    }

    @NonTransactional
    private boolean valEquals(V value1, V value2) {
        return value1 == null ? value2 == null : value1.equals(value2);
    }

    /**
     * Returns this map's entry for the given key, or <tt>null</tt> if the map
     * does not contain an entry for the key.
     *
     * @param key the key of the node to search.
     * @return this map's entry for the given key, or <tt>null</tt> if the map
     *         does not contain an entry for the key
     * @throws ClassCastException   if the specified key cannot be compared
     *                              with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *                              and this map uses natural ordering, or its comparator
     *                              does not permit null keys
     */
    final EntryImpl<K, V> getEntry(Object key) {
        // Offload comparator-based version for sake of performance
        if (comparator != null) {
            return findNodeUsingComparator(key);
        }

        if (key == null) {
            throw new NullPointerException();
        }

        Comparable<? super K> k = (Comparable<? super K>) key;
        EntryImpl<K, V> p = root;
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
     * {@inheritDoc}
     * <p/>
     * Version of findNode using comparator. Split off from findNode
     * for performance. (This is not worth doing for most methods,
     * that are less dependent on comparator performance, but is
     * worthwhile here.)
     */
    final EntryImpl<K, V> findNodeUsingComparator(Object key) {
        K k = (K) key;

        if (comparator != null) {
            EntryImpl<K, V> p = root;
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

    public boolean isBalanced() {
        return isBalanced(root);
    }

    @TransactionalMethod
    private static <K, V> EntryImpl<K, V> findMostLeft(EntryImpl<K, V> node) {
        if (node == null) {
            return null;
        }

        while (true) {
            EntryImpl<K, V> left = node.left;
            if (left == null) {
                return node;
            } else {
                node = left;
            }
        }
    }

    @TransactionalObject
    static class EntryImpl<K, V> implements Entry<K, V> {
        final K key;

        private V value;
        private EntryImpl<K, V> parent;
        private EntryImpl<K, V> left;
        private EntryImpl<K, V> right;
        private int height;

        EntryImpl(K key, V value) {
            this.key = key;
            this.value = value;
        }

        EntryImpl<K, V> rotateLeft() {
            EntryImpl<K, V> pivot = this.right;
            this.right = pivot.left;
            pivot.left = this;
            reassignParent(pivot);
            return pivot;
        }

        EntryImpl<K, V> rotateRight() {
            EntryImpl<K, V> pivot = this.left;
            this.left = pivot.right;
            pivot.right = this;
            reassignParent(pivot);
            return pivot;
        }

        EntryImpl<K, V> doubleRotateLeft() {
            rightRotateRightNode();
            return rotateLeft();
        }

        EntryImpl<K, V> doubleRotateRight() {
            leftRotateLeftNode();
            return rotateRight();
        }

        public boolean isLeftHeavy() {
            if (left == null) {
                return false;
            } else if (height(left) == 1) {
                return right == null;
            } else {
                return height(left) - height(right) > 1;
            }
        }

        public boolean isRightHeavy() {
            if (right == null) {
                return false;
            } else if (height(right) == 1) {
                return left == null;
            } else {
                return height(right) - height(left) > 1;
            }
        }

        public int height() {
            //the problem with this call is that the whole tree is loaded. So somehow
            //the height should be cached.
            return height(this);
        }

        public int balanceFactor() {
            return abs(height(right) - height(left));
        }

        private int height(EntryImpl<K, V> node) {
            if (node == null) {
                return 0;
            }

            int left = height(node.left);
            int right = height(node.right);
            return 1 + max(left, right);
        }

        private void rightRotateRightNode() {
            this.right = this.right.rotateRight();
        }

        private void leftRotateLeftNode() {
            this.left = this.left.rotateLeft();
        }

        private void reassignParent(EntryImpl<K, V> pivot) {
            pivot.parent = this.parent;
            this.parent = pivot;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public EntryImpl<K, V> getLeft() {
            return left;
        }

        public void setLeft(EntryImpl<K, V> left) {
            this.left = left;
        }

        public EntryImpl<K, V> getParent() {
            return parent;
        }

        public void setParent(EntryImpl<K, V> parent) {
            this.parent = parent;
        }

        public EntryImpl<K, V> getRight() {
            return right;
        }

        public void setRight(EntryImpl<K, V> right) {
            this.right = right;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            V old = this.value;
            this.value = value;
            return old;
        }

        @Override
        public K getKey() {
            return key;
        }
    }

    @TransactionalObject
    class EntryImplIterator implements Iterator<Entry<K, V>> {
        private EntryImpl<K, V> node;
        private Direction direction = Direction.self;

        EntryImplIterator() {
            this.node = root;
        }

        @Override
        public boolean hasNext() {
            return node != null;
        }

        @Override
        public Entry<K, V> next() {
            if (node == null) {
                throw new NoSuchElementException();
            }

            EntryImpl<K, V> result = node;

            switch (direction) {
                case self:
                    if (node.left != null) {
                        node = node.left;
                    } else if (node.right != null) {
                        node = node.right;
                    } else if (node.parent != null) {
                        //todo:
                    } else {
                        node = null;
                    }
                    break;
                case left:
                    if (node.right != null) {
                        node = node.right;
                        direction = Direction.self;
                    } else {
                        //todo
                    }
                    break;
                case right:

                    break;
                default:
                    throw new IllegalStateException();
            }


            if (node.right != null) {
                EntryImpl<K, V> mostLeft = findMostLeft(node.right);
                if (mostLeft != null) {
                    node = mostLeft;
                } else {
                    node = node.right;
                }
            } else {
                node = node.parent;
            }

            return result;
        }

        @Override
        public void remove() {
            throw new TodoException();
        }
    }

    enum Direction {
        left, right, self
    }

    @TransactionalObject
    final class EntrySet implements Set<Map.Entry<K, V>> {

        @Override
        public void clear() {
            TransactionalTreeMap.this.clear();
        }

        @Override
        public int size() {
            return TransactionalTreeMap.this.size();
        }

        @Override
        public boolean isEmpty() {
            return TransactionalTreeMap.this.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }

            Map.Entry<K, V> entry = (Map.Entry<K, V>) o;
            V value = entry.getValue();
            Entry<K, V> p = getEntry(entry.getKey());
            return p != null && valEquals(p.getValue(), value);
        }

        @Override
        public boolean add(Entry<K, V> entry) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends Entry<K, V>> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            if (c == null) {
                throw new NullPointerException();
            }

            if (c.isEmpty()) {
                return true;
            }

            for (Object item : c) {
                if (!contains(item)) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            boolean modified = false;

            if (size() > c.size()) {
                for (Object item : c) {
                    modified |= remove(item);
                }
            } else {
                for (Iterator<?> i = iterator(); i.hasNext();) {
                    if (c.contains(i.next())) {
                        i.remove();
                        modified = true;
                    }
                }
            }
            return modified;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new TodoException();
        }

        @Override
        public Object[] toArray() {
            throw new TodoException();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            throw new TodoException();
        }

        @Override
        public Iterator<Entry<K, V>> iterator() {
            return new EntryImplIterator();
        }

        @Override
        public boolean remove(Object o) {
            throw new TodoException();
        }

        @Override
        public String toString() {
            throw new TodoException();
        }

        @Override
        public int hashCode() {
            return TransactionalTreeMap.this.hashCode();
        }

        public boolean equals(Object thatObj) {
            if (thatObj == this) {
                return true;
            }

            if (!(thatObj instanceof Set)) {
                return false;
            }

            Set that = (Set) thatObj;
            if (that.size() != this.size()) {
                return false;
            }

            throw new TodoException();
        }
    }
}