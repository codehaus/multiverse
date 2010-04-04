package org.multiverse.transactional.nonblocking;

import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;

/**
 * A non blocking task can be compared to a {@link Runnable} except:
 * <ol>
 * <li>it works with a transaction</li>
 * <li>it can be retried. Retrying </li>
 * </ol>
 * <p/>
 * Should this be the same as context? So is there a need for context?
 *
 * @author Peter Veentjer.
 */
public interface NonBlockingTask {

    /**
     * The TransactionFactory used to createReference transactions for executing this task.
     *
     * @return the TransactionFactory used for creating transactions.
     */
    TransactionFactory getTransactionFactory();

    /**
     * Executes this non blocking task.
     *
     * @param t the Transaction used to execute the task.
     * @return true if the task should be executed again, false otherwise.
     * @throws org.multiverse.api.exceptions.Retry
     *
     */
    boolean execute(Transaction t);
}
