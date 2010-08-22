package org.multiverse.api;

import org.multiverse.api.blocking.Latch;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.transactions.BetaTransactionConfiguration;

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
 * {@link #register(TransactionLifecycleListener)}.
 * <p/>
 * When the Transaction is reset, the lifecycle tasks are dropped. So the lifecycle task need to be registered again.
 * </dd>
 * <p/>
 * <dt>Blocking</dt>
 * <dd>
 * It is possible to let a transaction block until some state changes has happened; comparable with
 * the waitset/{@link java.util.concurrent.locks.Condition} functionality already provided. For more information see the
 * {@link #registerChangeListenerAndAbort(org.multiverse.api.blocking.Latch)}
 * <p/>
 * Because a custom {@link org.multiverse.api.blocking.Latch} implementation can be provided, you have a lot of control
 * on the blocking behavior. But atm it is not possible to get fairness on when the Latch is opened. Policies to
 * customize starvation, lifelocking, deadlocking will be added in the future.
 * </dd>
 * <p/>
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
     * Commits this Transaction. If the Transaction is:
     * <ol>
     * <li>new: is is started and following the same flow as an active transaction. This is done so that
     * the lifecyclelisteners work.</li>
     * <li>active: it is prepared for commit and then committed</li>
     * <li>prepared: it is committed (so changes persisted)</li>
     * <li>aborted: a DeadTransactionException is thrown</li>
     * <li>committed: a DeadTransactionException is thrown</li>
     * </ol> So it is safe to call while active
     * or prepared.
     * <p/>
     * Transaction will be aborted if the commit does not succeed.
     * <p/>
     * Commit will not throw any validation exceptions after the transaction is prepared.
     *
     * @throws org.multiverse.api.exceptions.WriteConflict
     *          if the commit failed. Check the class hierarchy of the WriteConflict for more information.
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *          if the Transaction is not in the correct
     *          state for this operation.
     */
    void commit();

    /**
     * Aborts this Transaction. This means that the changes made in this transaction are not committed. It depends on
     * the implementation if this operation is simple (ditching objects for example), or if changes need to be rolled
     * back. If an exception is thrown while executing the ___abort, the transaction is still aborted. And example of
     * such a situation is a pre-___abort task that fails. So the transaction always is aborted (unless it is committed).
     * <p/>
     * If the Transaction already is aborted, the call is ignored.
     *
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *          if the Transaction is not in the correct state for this operation.
     */
    void abort();

    /**
     * Prepares this transaction to be committed. It can lock resources to make sure that no conflicting changes are
     * made after the transaction has been prepared. If the transaction already is prepared, the call is ignored.  If
     * the prepare fails, the transaction automatically is aborted. Once a transaction is prepared, it will always
     * succeed with the commit.
     * <p/>
     * It is very important that the transaction eventually commits or aborts, if it doesn't no other transaction
     * reading/writing the committed resources, can't commit.
     *
     * @throws org.multiverse.api.exceptions.WriteConflict
     *          if the transaction can't be prepared.
     * @throws org.multiverse.api.exceptions.DeadTransactionException
     *          if the transaction already is committed or aborted.
     */
    void prepare();

    /**
     * Signals that the only possible outcome of the Transaction is one that aborts. When the transaction prepares or
     * commits it checks if the transaction is marked for ___abort. If so, it will automatically aborted. If the
     * transaction is executed inside an AtomicBlock, it is automatically retried.
     * <p/>
     * The abortOnly marker is reset when a {@link #softReset()} or {@link #hardReset()} is done.
     *
     * This method is not threadsafe, so can only be called by the thread that used the transaction.
     *
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *          if the transaction is not in the correct
     *          stat for this operation.
     */
    void setAbortOnly();

    /**
     * Resets the transaction so that it can be reused for another attempt to evaluate the transaction. If
     * another attempt can be made, the attempt field will be increased.
     * <p/>
     * If the Transaction still is active/prepared, it will be aborted first.
     *
     * @return true if another attempt can be made, false otherwise.
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *          if the Transaction isn't in the correct
     *          state for this operation.
     */
    boolean softReset();

    /**
     * Resets the transaction so that it can be reused.
     */
    void hardReset();

    /**
     * todo: this operation should only be allowed when it is in the new state?
     *
     * todo: instead of accepting a BetaTransactionConfiguration, TransactionConfiguration should be used.
     *
     * @param transactionConfiguration the TransactionConfiguration to initialize the Transaction with.
     * @throws NullPointerException if transactionConfiguration is null.
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *                              when the transaction isn't in the
     *                              correct state for this operation.
     */
    void init(BetaTransactionConfiguration transactionConfiguration);

    /**
     * Registers a permanent TransactionLifecycleListener.
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

    /**
     * Gets the remaining timeout in nanoseconds. Long.MAX_VALUE indicates that no timeout should be used.
     *
     * @return the remaining timeout.
     */
    long getRemainingTimeoutNs();

    /**
     * Sets the remaining timeout in nanoseconds. Long.MAX_VALUE indicates that no timeout should be used. When
     * the Transaction is used for the first attempt, the remaining timeout is set to the
     * {@link org.multiverse.api.TransactionConfiguration#getTimeoutNs()}.
     * <p/>
     * This normally isn't called from the user code, it is task of the stm internals and the
     * transaction management to use the timeout.
     *
     * @param timeoutNs the timeout.
     * @throws IllegalArgumentException if timeout smaller than 0 or when the timeout is larger than the previous
     *                                  remaining timeout. This is done to prevent that the timeout is increased
     *                                  to a value that is in conflict with the {@link TransactionConfiguration}.
     */
    void setRemainingTimeoutNs(long timeoutNs);

    /**
     * Registers the changeListener to all reads done by the transaction and aborts the transaction. This functionality
     * is needed for creating blocking transactions; transactions that are able to wait for change. In the STM literature
     * this is know as the 'retry' and 'orelse' functionality.
     *
     * @param changeListener the Latch the is notified when a change happens on one of the reads.
     * @throws NullPointerException if changeListener is null (will also ___abort the transaction).
     * @throws org.multiverse.api.exceptions.NoRetryPossibleException
     *                              if no registration can be done because the
     *                              transaction doesn't have any reads. When this exception happens, the transaction
     *                              will also be aborted.
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *                              if the transaction isn't in the correct
     *                              state for this transaction. When this exception happens, the transaction will also
     *                              be aborted (if it isn't committed).
     */
    void registerChangeListenerAndAbort(Latch changeListener);

    void startEitherBranch();

    void endEitherBranch();

    void startOrElseBranch();
}
