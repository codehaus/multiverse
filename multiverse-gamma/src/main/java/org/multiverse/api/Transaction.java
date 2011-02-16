package org.multiverse.api;

import org.multiverse.api.lifecycle.TransactionListener;

/**
 * The unit of work for {@link Stm}. The transaction make sure that changes on {@link TransactionalObject} instances are:
 * <ol>
 * <li>Atomic: all or nothing gets committed (Failure atomicity)</li>
 * <li>Consistent : </li>
 * <li>Isolated: a transaction is executed isolated from other transactions. Meaning that a transaction won't see changed made by
 * transactions executed concurrently, but it will see changes made by transaction completed before. It depends on the
 * {@link IsolationLevel} or {@link LockMode} used how strict the isolation is.</li>
 * </ol>
 *
 * <h3>Thread-safety</h3>
 *
 * <p>A Transaction is not thread-safe (just like a Hibernate Session is not thread-safe to use). It can be
 * handed over from thread to thread, but one needs to be really careful with the {@link ThreadLocalTransaction} or other
 * thread specific state like the stackframe of a method (this is an issue when instrumentation is used since the stackframe
 * is likely to be enhanced to include the Transaction as a local variable.
 *
 * <h3>TransactionListener</h3>
 *
 * <p>It is possible to listen to a Transaction when it aborts/prepares/commits/starts. There are 2 different flavors of
 * listeners:
 * <ol>
 * <li>normal listeners: are registered during the execution of a transaction using the
 * {@link Transaction#register(org.multiverse.api.lifecycle.TransactionListener)} method. If the transactions aborts/commits
 * these listeners are removed. So if the transaction is retried, the listeners need to be registered (this is easy since
 * the logic inside the atomic block that did the register, is executed again.
 * </li>
 * <li>permanent listeners: are registered once and will always remain. It can be done on the
 * AtomicBlock level using the {@link TransactionFactoryBuilder#addPermanentListener(org.multiverse.api.lifecycle.TransactionListener)}
 * or it can be done on the Stm level.
 * </li>
 * </ol>
 *
 * This can be done with the
 * {@link #register(org.multiverse.api.lifecycle.TransactionListener)}. You can also register permanent TransactionListener;
 * useful for a product that uses Multiverse as lower level stm and always want to execute certain functionality, look for the
 * {@link TransactionFactoryBuilder#addPermanentListener(org.multiverse.api.lifecycle.TransactionListener).
 * <p>
 * When the Transaction is reset, the normal TransactionListeners are dropped and the permanent are not. So the normal
 * TransactionListener need to be registered again (this is easy because the transaction is executed again.
 *
 * <h3>Storing transaction references</h3>
 *
 * <p>Transaction instances should not be stored since they are likely to be pooled by the STM. So it could be that the same
 * transaction instance is re-used to execute a completely unrelated piece of logic, and it can also be that different instances
 * are used to execute the same logic.
 *
 * @author Peter Veentjer.
 */
public interface Transaction {

    /**
     * Returns the TransactionConfiguration used by this Transaction.
     * <p/>
     * Because the Transaction can be reused, the TransactionConfiguration used by this Transaction doesn't need to be constant.
     *
     * @return the TransactionConfiguration.
     */
    TransactionConfiguration getConfiguration();

    /**
     * Returns the status of this Transaction.
     *
     * @return the status of this Transaction.
     */
    TransactionStatus getStatus();

    /**
     * Gets the current attempt (so the number of tries this transaction already had). Value will
     * always be equal or larger than 1 (the first attempt returns 1). The maximum number of attempts for retrying is determined based
     * on the  {@link TransactionConfiguration#getMaxRetries()}
     *
     * @return the current attempt.
     */
    int getAttempt();

    /**
     * Gets the remaining timeout in nanoseconds. Long.MAX_VALUE indicates that no timeout is used.
     * <p/>
     * The remaining timeout only is decreased if a transaction blocks.
     *
     * @return the remaining timeout.
     */
    long getRemainingTimeoutNs();

    /**
     * Commits this Transaction. If the Transaction is:
     * <ol>
     * <li>active: it is prepared for commit and then committed</li>
     * <li>prepared: it is committed (so changes persisted). Once it is prepared, the commit is guaranteed to
     * succeed.</li>
     * <li>aborted: a DeadTransactionException is thrown</li>
     * <li>committed: a DeadTransactionException is thrown</li>
     * </ol>
     * So it is safe to call while active or prepared.
     * <p/>
     * Transaction will always be aborted if the commit does not succeed.
     * <p/>
     * Commit will not throw any validation exceptions after the transaction is prepared.
     * <p/>
     * If there are TransactionListeners (either normal ones or permanent ones) and they thrown a RuntimeException
     * or Error, this will be rethrown.
     *
     * @throws org.multiverse.api.exceptions.ReadWriteConflict
     *          if the commit failed. Check the class hierarchy of the ReadWriteConflict for more information.
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *          if the Transaction is not in the correct
     *          state for this operation.
     */
    void commit();

    /**
     * Prepares this transaction to be committed. It can lock resources to make sure that no conflicting changes are
     * made after the transaction has been prepared. If the transaction already is prepared, the call is ignored.  If
     * the prepare fails, the transaction automatically is aborted. Once a transaction is prepared, the commit will
     * always succeed.
     * <p/>
     * It is very important that the transaction eventually commits or aborts, if it doesn't no other transaction
     * reading/writing the committed resources, can't commit.
     *
     * @throws org.multiverse.api.exceptions.ReadWriteConflict
     *          if the transaction can't be prepared.
     * @throws org.multiverse.api.exceptions.DeadTransactionException
     *          if the transaction already is committed or aborted.
     */
    void prepare();

    /**
     * Aborts this Transaction. This means that the changes made in this transaction are not committed. It depends on
     * the implementation if this operation is simple (ditching objects for example), or if changes need to be rolled
     * back. If an exception is thrown while executing the abort, the transaction is still aborted. And example of
     * such a situation is a pre-abort task that fails. So the transaction always is aborted (unless it is committed).
     * <p/>
     * If the Transaction already is aborted, the call is ignored.
     *
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *          if the Transaction is not in the correct state for this operation.
     */
    void abort();

    /**
     * Retries the transaction. This call doesn't block, but if all goes well a RetryError is thrown which is caught by
     * the AtomicBlock.
     *
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *          if the transaction is not in a legal state for
     *          this operation.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    void retry();

    /**
     * Signals that the only possible outcome of the Transaction is one that aborts. When the transaction prepares or
     * commits it checks if the transaction is marked for abort. If so, it will automatically aborted and an
     * {@link org.multiverse.api.exceptions.AbortOnlyException} is thrown.
     * <p/>
     * This method is not threadsafe, so can only be called by the thread that used the transaction.
     *
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *          if the transaction is not active.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    void setAbortOnly();

    /**
     * Checks if this Transaction is abortonly (so will always fail when committing or preparing).
     * <p/>
     * This method is not threadsafe, so can only be called by the thread that used the transaction.
     *
     * @return true if abortonly, false otherwise.
     * @throws org.multiverse.api.exceptions.DeadTransactionException
     *          if the transaction is committed/aborted.
     */
    boolean isAbortOnly();

    /**
     * Registers a TransactionListener. Every time a transaction is retried, the listener needs to
     * be registered again if you want the task to be executed again. If you want a permanent listener, have
     * a look at the {@link TransactionFactoryBuilder#addPermanentListener(org.multiverse.api.lifecycle.TransactionListener)}.
     * <p/>
     * If a TransactionListener is added more than once, it is executed more than once. No checks
     * are made. The permanent listeners are executed in the order they are added.
     * <p/>
     * If a TransactionListener throws an Error/RuntimeException and the transaction still is alive,
     * it is aborted. For compensating and deferred actions this is not an issue, but for the PrePrepare state
     * or the state it could since the transaction is aborted.
     *
     * @param listener the listener to add.
     * @throws NullPointerException if listener is null. If the transaction is still alive, it is aborted.
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *                              if the transaction is not in the correct
     *                              state (e.g. aborted or committed).
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    void register(TransactionListener listener);
}
