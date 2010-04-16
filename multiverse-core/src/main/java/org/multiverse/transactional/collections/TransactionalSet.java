package org.multiverse.transactional.collections;

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

}
