package org.multiverse.api;

import java.util.List;

/**
 *
 * @author Peter Veentjer.
 */
public interface TwoPhaseCommitGroup {

    /**
     * The transaction that belongs to this 2phase commit.
     *
     * @return
     */
    List<Transaction> getTransaction();

    TransactionStatus getStatus();
}
