package org.multiverse.api.references;

import org.multiverse.api.TransactionalObject;

/**
 * A Transactional Reference.
 *
 * @param <E>
 */
public interface Ref<E> extends TransactionalObject{

    E atomicGet();
}
