package org.multiverse.durability;

/**
 * Mechanism to persist state. The Storage itself doesn't provide much concurrency control, it is the task
 * of the entity and the transaction to prevent any isolation problems to happen.
 *
 * @author Peter Veentjer.
 */
public interface Storage {

    /**
     * Loads a fully initialized DurableObject with the provided id. If such an object already is in memory
     * it is returned, if it isn't, it is loaded from storage. So you will get the guarantee that object
     * identity is maintained.
     *
     * @param id the id of the DurableObject to load.
     * @return the loaded DurableObject.
     * @throws StorageException     if something fails while loading
     * @throws NullPointerException if id is null.
     */
    DurableObject loadDurableObject(String id);

    /**
     * Starts a unit of work. Essentially this is the transaction for the storage mechanism.
     *
     * @return the started unit of work.
     */
    UnitOfWrite startUnitOfWrite();

    /**
     * Removes all persistent objects.
     * <p/>
     * This method is only here for debugging purposes.
     */
    void clear();
}
