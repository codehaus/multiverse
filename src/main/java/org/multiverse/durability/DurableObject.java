package org.multiverse.durability;

/**
 * Interface for a DurableObject: an object that can be persisted on disk.
 *
 * @author Peter Veentjer
 */
public interface DurableObject {

    /**
     * Returns the storage id.
     * <p/>
     * todo: what does it mean when null is returned?
     * todo: can this value only be read when it is unlocked?
     *
     * @return   the storage id.
     */
    String ___getStorageId();

    /**
     * Sets the storage id.
     *
     * @param id the storageId
     */
    void ___setStorageId(String id);

    /**
     * Marks the current object as durable. It can only be marked when the object is locked,
     * so it can't happen that it is going to be marked as durable if already locked by another
     * transaction.
     */
    void ___markAsDurable();

    /**
     * Checks if this DurableObject is durable. This call can only be made when the object already
     * is locked.
     *
     * @return true if it is durable, false otherwise.
     */
    boolean ___isDurable();
}
