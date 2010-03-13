package org.multiverse.transactional.collections;

import org.multiverse.annotations.TransactionalMethod;

import java.util.List;
import java.util.ListIterator;

/**
 * A {@link org.multiverse.transactional.collections.TransactionalCollection} that implements the {@link List}
 * interface.
 *
 * @author Peter Veentjer.
 * @param <E>
 * @see org.multiverse.transactional.collections.TransactionalCollection
 * @see java.util.List
 * @see java.util.Collection
 */
public interface TransactionalList<E> extends List<E>, TransactionalCollection<E> {

    @Override
    @TransactionalMethod(readonly = true)
    E get(int index);

    @Override
    @TransactionalMethod(readonly = true)
    int indexOf(Object o);

    @Override
    @TransactionalMethod(readonly = true)
    int lastIndexOf(Object o);

    @Override
    @TransactionalMethod(readonly = true)
    ListIterator<E> listIterator();

    @Override
    @TransactionalMethod(readonly = true)
    ListIterator<E> listIterator(int index);

    @Override
    @TransactionalMethod(readonly = true)
    List<E> subList(int fromIndex, int toIndex);
}
