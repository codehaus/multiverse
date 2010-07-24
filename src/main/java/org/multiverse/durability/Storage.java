package org.multiverse.durability;

/**
 * Mechanism to persist state. The Storage itself doesn't provide much concurrency control, it is the task
 * of the entity and the transaction to prevent any isolation problems to happen.
 *
 * @author Peter Veentjer.
 */
public interface Storage {

    /**
     * Loads the DurableObject with the specified id. The state of the Object is not loaded.
     *
     * @param id the id of the DurableObject to load.
     * @return the loaded DurableObject
     * @throws NullPointerException if id is null.
     */
    DurableObject loadDurableObject(String id);

    /**
     * Loads the state for the specific id.
     *
     * @param id the id of the DurableObject to load the state for.
     * @return the loaded state.
     * @throws NullPointerException if id is null.
     */
    DurableState loadState(String id);

    /**
     * Starts a unit of work. Essentially this is the transaction for the storage mechanism.
     *
     * @return the started unit of work.
     */
    UnitOfWork startUnitOfWork();

    /**
     * Removes all persistent objects.
     *
     * This method is only here for debugging purposes.
     */
    void clear();
}
