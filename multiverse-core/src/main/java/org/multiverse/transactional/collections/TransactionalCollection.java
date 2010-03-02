package org.multiverse.transactional.collections;

import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;

import java.util.Collection;
import java.util.Iterator;

/**
 * A Transactional version of the {@link Collection} interface.
 *
 * @author Peter Veentjer.
 * @param <E>
 */
@TransactionalObject
public interface TransactionalCollection<E> extends Collection<E> {

    @Override
    @TransactionalMethod(readonly = true)
    int size();

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
}
