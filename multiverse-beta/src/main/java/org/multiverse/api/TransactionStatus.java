package org.multiverse.api;

/**
 * An enumeration of all possible states a {@link Transaction} can be in.
 *
 * @author Peter Veentjer
 */
public enum TransactionStatus {

    /**
     * When a Transaction is running.
     */
    Active(true),

    /**
     * When a Transaction is prepared for commit. Once it reaches this state, a commit will always
     * happen.
     */
    Prepared(true),

    /**
     * When a Transaction failed to commit.
     */
    Aborted(false),

    /**
     * When a Transaction committed.
     */
    Committed(false);

    private final boolean alive;

    TransactionStatus(boolean alive) {
        this.alive = alive;
    }

    /**
     * Checks if the Transaction still is active/prepared.
     *
     * @return true if the TransactionStatus is active or prepared.
     */
    public boolean isAlive() {
        return alive;
    }
}
