package org.multiverse.api;

/**
 * An enumeration for all possible events for the transaction lifecycle.
 *
 * @author Peter Veentjer. 
 */
public enum TransactionLifecycleEvent {

    /**
     * Just before aborting.
     */
    preAbort,

    /**
     * Just before committing.
     */
    preCommit,

    /**
     * Just after aborting.
     */
    postAbort,

    /**
     * Just after committing.
     */
    postCommit
}
