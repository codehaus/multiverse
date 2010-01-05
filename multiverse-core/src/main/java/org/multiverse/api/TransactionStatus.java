package org.multiverse.api;

/**
 * An enumeration containing the different states a {@link Transaction} can be in. Every
 * transaction always start with the active status. If the transaction committed successfully,
 * the status will change to committed. Or when it is is aborted, the status will change to
 * aborted.
 *
 * If in the future an unstarted state is wanted, please fill in a request for enhancement or
 * go to the mailinglist to place your question.
 *
 * @author Peter Veentjer.
 * @see Transaction#getStatus()
 */
public enum TransactionStatus {

    active, committed, aborted
}
