package org.multiverse.api;

/**
 * An enumeration containing the different states a {@link Transaction} can be in. Every
 * transaction begins with the active status. If the transaction committed successfully, the status
 * will change to committed. Or when it is is aborted, the status will change to aborted.
 *
 * @author Peter Veentjer.
 * @see Transaction#getStatus()
 */
public enum TransactionStatus {

    active, committed, aborted
}
