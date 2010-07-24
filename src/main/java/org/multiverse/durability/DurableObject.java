package org.multiverse.durability;

/**
 * Interface for a DurableObject: an object that can be persisted on disk.
 *
 * @author Peter Veentjer
 */
public interface DurableObject {

    String getStorageId();

    void setStorageId(String id);


}
