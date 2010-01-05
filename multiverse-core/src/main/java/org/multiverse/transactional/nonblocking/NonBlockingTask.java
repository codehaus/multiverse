package org.multiverse.transactional.nonblocking;

import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;

/**
 * A task that can be executed by a {@link NonBlockingTransactionExecutor}.
 *
 * @author Peter Veentjer.
 */
public interface NonBlockingTask {

    /**
     * The TransactionFactory used to create transactions for executing this task.
     *
     * @return the TransactionFactory used for creating transactions.
     */
    TransactionFactory getTransactionFactory();

    /**
     *
     * @param t the Transaction used to execute the task.
     * @return true if the task should be executed again, false otherwise.
     * @throws org.multiverse.api.exceptions.RetryError
     */
    boolean execute(Transaction t);
}
