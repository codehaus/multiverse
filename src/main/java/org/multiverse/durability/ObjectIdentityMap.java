package org.multiverse.durability;

/**
 * A kind of hashmap that is responsible for storing DurableObjects for some time in memory. So one of its
 * responsibility is to act as a kind of cache. Another responsibility is to make sure that object identity
 * is guaranteed.
 *
 * @author Peter Veentjer.
 */
public interface ObjectIdentityMap {

    /**
     * Gets the object with the specified id.
     *
     * @param id the id of the DurableObject to load.
     * @return the DurableObject, or null is none is found.
     * @throws NullPointerException if id is null.
     */
    DurableObject get(String id);

    /**
     * Puts the entity in the ObjectIdentityMap if it not already stored. If it already is stored, the
     * stored value is returned.
     *
     * @param object the DurableObject to place int his Map.
     * @return null if no other DurableObject is stored with the same id, or the current stored DurableObject.
     * @throws NullPointerException if object is null.
     */
    DurableObject putIfAbsent(DurableObject object);

    /**
     * Removes all loaded DurableObjects. Method should only be used for debugging purposes.
     */
    void clear();
}
