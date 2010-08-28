package org.multiverse.api.references;

import org.multiverse.api.TransactionalObject;

/**
 * A Transactional reference for managing an int.
 */
public interface IntRef extends TransactionalObject {

    int atomicGet();
}
