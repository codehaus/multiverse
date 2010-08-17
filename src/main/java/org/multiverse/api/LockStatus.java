package org.multiverse.api;

/**
 * An enumeration of all status a lock can be in
 *
 * @author Peter Veentjer.
 */
public enum LockStatus {

    /**
     * Not locked by anyone
     */
    Free,

    /**
     * Locked by another transaction
     */
    LockedByOther,

    /**
     * Locked by itself.
     */
    LockedBySelf
}
