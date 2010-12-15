package org.multiverse.api.lifecycle;

/**
 * An enumeration for all possible events for the transaction lifecycle.
 *
 * @author Peter Veentjer.
 */
public enum TransactionLifecycleEvent {

    /**
     * Just before starting.
     */
    PreStart,

    /**
     * Just after starting.
     */
    PostStart,

    /**
     * Just before preparing
     */
    PrePrepare,

    /**
     * Just after aborting.
     */
    PostAbort,

    /**
     * Just after committing.
     */
    PostCommit
}
