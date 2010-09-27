package org.multiverse.api;

/**
 * A Watch can be used to listen to a change made on a transactional object.
 * <p/>
 * Not used yet.
 *
 * @author Peter Veentjer.
 */
public interface Watch<T extends TransactionalObject> {

    void execute(T transactionalObject);
}
