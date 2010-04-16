package org.multiverse.transactional.collections;

import org.multiverse.annotations.Exclude;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;

import java.util.Collection;
import java.util.Iterator;

/**
 * A Transactional version of the {@link Collection} interface.
 *
 * @author Peter Veentjer.
 * @param <E>
 * @see java.util.Collection
 */
@TransactionalObject
public interface TransactionalCollection<E> extends Collection<E> {

    @Override
    @TransactionalMethod(readonly = true)
    int size();

    /**
     * Returns the current size of the TransactionalCollection. The big difference between
     * the normal {@link #size()} method is that this one returns the actual size of
     * this map and doesn't look at the current transaction. So you could see changes
     * made by other threads.
     *
     * @return the current size of the TransactionalMap.
     */
    @Exclude
    int currentSize();

    @Override
    @TransactionalMethod(readonly = true)
    boolean isEmpty();

    @Override
    @TransactionalMethod(readonly = true)
    boolean contains(Object o);

    @Override
    @TransactionalMethod(readonly = true)
    Iterator<E> iterator();

    @Override
    @TransactionalMethod(readonly = true)
    Object[] toArray();

    @Override
    @TransactionalMethod(readonly = true)
    <T> T[] toArray(T[] a);

    @Override
    @TransactionalMethod(readonly = true)
    boolean containsAll(Collection<?> c);

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
    boolean add(E e);

    @Override
    boolean remove(Object o);

    @Override
    boolean addAll(Collection<? extends E> c);

    @Override
    boolean removeAll(Collection<?> c);

    @Override
    boolean retainAll(Collection<?> c);

    @Override
    void clear();

}
