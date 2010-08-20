package org.multiverse.durability;

import java.util.Iterator;

/**
 * The state (the tranlocal) of a durability object.
 *
 * @author Peter Veentjer
 */
public interface DurableState<D extends DurableObject> {

    /**
     * Returns an iterator over all DurableObject that can be reached from this State.
     *
     * @return an iterator containing all durable objects.
     */
    Iterator<DurableObject> getReferences();

    D getOwner();
}
