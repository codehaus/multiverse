package org.multiverse.api;

import org.multiverse.api.lifecycle.TransactionLifecycleListener;

/**
 * All changes on transaction objects must be done through a Transaction. The transaction make sure that changes
 * on java objects are:
 * <ul>
 * <li>Atomic: all or nothing gets committed (Failure atomicity)</li>
 * <li>Consistent : </li>
 * <li>Isolated: a
 * transaction is executed isolated from other transactions. Meaning that a transaction won't see changed made by
 * transactions executed concurrently, but it will see changes made by transaction completed before.</li>
 * </ul>
 * <p/>
 * <dl>
 * <p/>
 * <dt>Thread-safety</dt>
 * <dd>
 * A Transaction is not thread-safe to use (just like a Hibernate Session is not thread-safe to use). It can be
 * handed over from thread to thread, but one needs to be really careful with threadlocals. Although the
 * Stm/Transaction implementation don't care about threadlocals, the stuff in front (templates, instrumented code etc)
 * could depend on threadlocals.
 * </dd>
 * <p/>
 * <dt>TransactionLifecycle</dt>
 * <dd>
 * It is possible to listen to a Transaction when it aborts, or commits. This can be done with the
 * {@link #register(TransactionLifecycleListener)}.* You can also register permanent lifecycle listeners (useful for a
 * product that uses Multiverse as lower level stm, look for the
 * {@link TransactionFactoryBuilder#addPermanentListener(TransactionLifecycleListener)}.
 * <p/>
 * When the Transaction is reset, the normal lifecycle tasks are dropped and the permanent are not. So the lifecycle
 * task need to be registered again (this is easy because the transaction is executed again).
 * </dd>
 * </dl>
 *
 * @author Peter Veentjer.
 */
public interface Transaction {

    /**
     * Returns the TransactionConfiguration used by this Transaction.
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
     * always be equal or larger than 1.
     *
     * @return the current attempt.
     */
    int getAttempt();

    /**
     * Gets the remaining timeout in nanoseconds. Long.MAX_VALUE indicates that no timeout is used.
     *
     * @return the remaining timeout.
     */
    long getRemainingTimeoutNs();

    /**
     * Commits this Transaction. If the Transaction is:
     * <ol>
     * <li>new: is is activated and following the same flow as an active transaction. This is done so that
     * the lifecyclelisteners work.</li>
     * <li>active: it is prepared for commit and then committed</li>
     * <li>prepared: it is committed (so changes persisted). Once it is prepared, the commit is guaranteed to
     * succeed.</li>
     * <li>aborted: a DeadTransactionException is thrown</li>
     * <li>committed: a DeadTransactionException is thrown</li>
     * </ol> So it is safe to call while active
     * or prepared.
     * <p/>
     * Transaction will be aborted if the commit does not succeed.
     * <p/>
     * Commit will not throw any validation exceptions after the transaction is prepared.
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

    void retry();

    /**
     * Signals that the only possible outcome of the Transaction is one that aborts. When the transaction prepares or
     * commits it checks if the transaction is marked for abort. If so, it will automatically aborted and an
     * {@link org.multiverse.api.exceptions.ExplicitAbortException} is thrown.
     * <p/>
     * This method is not threadsafe, so can only be called by the thread that used the transaction.
     *
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *          if the transaction is not active.
     */
    void setAbortOnly();

    /**
     * Checks if this Transaction is abortonly.
     * <p/>
     * This method is not threadsafe, so can only be called by the thread that used the transaction.
     *
     * @return true if abortonly, false otherwise.
     * @throws org.multiverse.api.exceptions.DeadTransactionException
     *          if the transaction is committed/aborted.
     */
    boolean isAbortOnly();

    /**
     * Ensures all writes that have been made. If one or more of the ref have been privatized, this call will also
     * complete successfully. After this call completes successfully, the transaction still can be used (unlike the
     * {@link #prepare()}.
     *
     * @throws org.multiverse.api.exceptions.ReadWriteConflict
     *          if one or more of the transactional objects
     *          can't be ensured.
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *          if the transaction is not in the correct
     *          state for this operation.
     */
    void ensureWrites();

    /**
     * Registers a TransactionLifecycleListener. Every time a transaction is retried, the listener needs to
     * be registered again if you want the task to be executed again. If you want a permanent listener, have
     * a look at the {@link TransactionFactoryBuilder#addPermanentListener(TransactionLifecycleListener)}.
     * <p/>
     * If a TransactionLifecycleListener is added more than once, it is executed more than once. No checks
     * are made. The permanent listeners are executed in the order they are added.
     *
     * @param listener the listener to add.
     * @throws NullPointerException if listener is null
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *                              if the transaction is not in the correct
     *                              state (e.g. aborted or committed).
     */
    void register(TransactionLifecycleListener listener);
}
