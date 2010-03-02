package org.multiverse.transactional.collections;

import java.util.Set;

/**
 * A Transactional version of the {@link Set} interface.
 *
 * @author Peter Veentjer
 */
public interface TransactionalSet<E> extends Set<E>, TransactionalCollection<E> {

}
