package org.multiverse.api.references;

import org.multiverse.api.TransactionalObject;

/**
 * A transactional reference for a long.
 */
public interface LongRef extends TransactionalObject {

    long atomicGet();
}
