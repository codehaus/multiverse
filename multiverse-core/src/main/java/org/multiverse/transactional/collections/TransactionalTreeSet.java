package org.multiverse.transactional.collections;

import org.multiverse.annotations.NonTransactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;

/**
 * A {@link TransactionalSet} that is backed up by a {@link TransactionalTreeMap}. It is the
 * transactional version of the {@link java.util.TreeSet}.
 *
 * @author Peter Veentjer
 * @see TransactionalSet
 * @see java.util.Set
 */
public final class TransactionalTreeSet<E> implements TransactionalSet<E> {

    private final static Object VALUE = new Object();

    private final TransactionalTreeMap<E, Object> map;

    /**
     * Constructs a new, empty TransactionalTreeSet, sorted according to the natural ordering of its
     * elements.  All elements inserted into the set must implement the {@link Comparable}
     * interface. Furthermore, all such elements must be <i>mutually comparable</i>:
     * {@code e1.compareTo(e2)} must not throw a {@code ClassCastException} for any elements
     * {@code e1} and {@code e2} in the set.  If the user attempts to add an element
     * to the set that violates this constraint (for example, the user attempts to add a
     * string element to a set whose elements are integers), the {@code add} call will throw a
     * {@code ClassCastException}.
     */
    public TransactionalTreeSet() {
        map = new TransactionalTreeMap<E, Object>();
    }

    /**
     * Constructs a new, empty TransactionalTreeSet, sorted according to the specified
     * comparator.  All elements inserted into the set must be <i>mutually
     * comparable</i> by the specified comparator: {@code comparator.compare(e1,
     *e2)} must not throw a {@code ClassCastException} for any elements
     * {@code e1} and {@code e2} in the set.  If the user attempts to add
     * an element to the set that violates this constraint, the
     * {@code add} call will throw a {@code ClassCastException}.
     *
     * @param comparator the comparator that will be used to order this set.
     *                   If {@code null}, the {@linkplain Comparable natural
     *                   ordering} of the elements will be used.
     */
    public TransactionalTreeSet(Comparator<? super E> comparator) {
        map = new TransactionalTreeMap<E, Object>(comparator);
    }

    /**
     * Constructs a new TransactionalTreeSet containing the elements in the specified
     * collection, sorted according to the <i>natural ordering</i> of its
     * elements.  All elements inserted into the set must implement the
     * {@link Comparable} interface.  Furthermore, all such elements must be
     * <i>mutually comparable</i>: {@code e1.compareTo(e2)} must not throw a
     * {@code ClassCastException} for any elements {@code e1} and
     * {@code e2} in the set.
     *
     * @param c collection whose elements will comprise the new set
     * @throws ClassCastException   if the elements in {@code c} are
     *                              not {@link Comparable}, or are not mutually comparable
     * @throws NullPointerException if the specified collection is null
     */
    public TransactionalTreeSet(Collection<? extends E> c) {
        if (c == null) {
            throw new NullPointerException();
        }

        map = new TransactionalTreeMap<E, Object>();
        addAll(c);
    }

    @Override
    public boolean add(E e) {
        return map.put(e, VALUE) == null;
    }

    @Override
    @NonTransactional
    public int atomicSize() {
        return map.getCurrentSize();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    @Override
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    @Override
    public Object[] toArray() {
        return map.keySet().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return map.keySet().toArray(a);
    }

    @Override
    public boolean remove(Object o) {
        return map.remove(o) != null;
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
    public void clear() {
        map.clear();
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        if (c == null) {
            throw new NullPointerException();
        }

        if (c.isEmpty()) {
            return false;
        }

        boolean changed = false;
        for (E item : c) {
            if (add(item)) {
                changed = true;
            }
        }

        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        if (c == null) {
            throw new NullPointerException();
        }

        if (c.isEmpty()) {
            if (isEmpty()) {
                return false;
            } else {
                clear();
                return true;
            }
        }

        boolean modified = false;
        Iterator<E> e = iterator();
        while (e.hasNext()) {
            if (!c.contains(e.next())) {
                e.remove();
                modified = true;
            }
        }

        return modified;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        if (c == null) {
            throw new NullPointerException();
        }

        if (c.isEmpty()) {
            return false;
        }

        boolean changed = false;
        for (Object item : c) {
            if (remove(item)) {
                changed = true;
            }
        }

        return changed;
    }

    @Override
    public boolean equals(Object thatObj) {
        if (thatObj == this) {
            return true;
        }

        if (!(thatObj instanceof Set)) {
            return false;
        }

        Set that = (Set) thatObj;
        if (that.size() != size()) {
            return false;
        }

        try {
            return containsAll(that);
        } catch (ClassCastException unused) {
            return false;
        } catch (NullPointerException unused) {
            return false;
        }
    }

    @Override
    public String toString() {
        if (map.isEmpty()) {
            return "[]";
        }

        return map.keySet().toString();
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }
}
