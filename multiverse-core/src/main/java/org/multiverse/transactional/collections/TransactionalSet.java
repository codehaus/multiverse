package org.multiverse.transactional.collections;

import java.util.Collection;
import java.util.Set;

/**
 * A Transactional version of the {@link Set} interface.
 *
 * @author Peter Veentjer
 * @see org.multiverse.transactional.collections.TransactionalCollection
 * @see java.util.Collection
 * @see java.util.Set
 */
public interface TransactionalSet<E> extends Set<E>, TransactionalCollection<E> {

    @Override
    boolean add(E e);

    @Override
    boolean remove(Object o);

    @Override
    boolean addAll(Collection<? extends E> c);

    @Override
    boolean retainAll(Collection<?> c);

    @Override
    boolean removeAll(Collection<?> c);

    @Override
    void clear();
}
