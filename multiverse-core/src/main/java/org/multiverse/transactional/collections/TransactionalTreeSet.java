package org.multiverse.transactional.collections;

import org.multiverse.utils.TodoException;

import java.util.Collection;
import java.util.Iterator;

/**
 * A {@link TransactionalSet} that is backed up by a {@link TransactionalTreeMap}
 *
 * @author Peter Veentjer
 */
public class TransactionalTreeSet<E> implements TransactionalSet<E> {

    private final static Object VALUE = new Object();

    private final TransactionalTreeMap<E, Object> map = new TransactionalTreeMap<E, Object>();

    @Override
    public boolean add(E e) {
        return map.put(e, VALUE) == null;
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
        throw new TodoException();
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        throw new TodoException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new TodoException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new TodoException();
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public String toString() {
        return map.keySet().toString();
    }

    @Override
    public boolean equals(Object o) {
        throw new TodoException();
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }
}
