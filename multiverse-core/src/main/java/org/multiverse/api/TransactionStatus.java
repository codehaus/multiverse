package org.multiverse.api;

/**
 * An enumeration containing the different states a {@link Transaction} can be in. Every transaction always start with
 * the active status. If the transaction committed successfully, the status will change to committed. Or when it is is
 * aborted, the status will change to aborted.
 * <p/>
 * If in the future an unstarted state is wanted, please fill in a request for enhancement or go to the mailinglist to
 * place your question.
 *
 * @author Peter Veentjer.
 * @see Transaction#getStatus()
 */
public enum TransactionStatus {

    New(false),

    Active(false),

    Prepared(false),

    Committed(true),

    Aborted(true);

    private final boolean isDead;

    private TransactionStatus(boolean isDead) {
        this.isDead = isDead;
    }

    /**
     * Checks if the TransactionStatus belongs to a dead (committed or aborted) transaction.
     *
     * @return true if dead, false otherwise.
     */
    public final boolean isDead() {
        return isDead;
    }
}
