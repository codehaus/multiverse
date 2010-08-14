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
     * Just after preparing
     */
    PostPrepare,

    /**
     * Just after aborting.
     */
    PostAbort,


    /**
     * Just before committing.
     */
    PreCommit,


    /**
     * Just after committing.
     */
    PostCommit
}
