package org.multiverse.api;

import org.multiverse.utils.latches.Latch;

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
 * <dt>Listen to lifecycles</dt>
 * <dd>
 * It is possible to listen to a Transaction when it aborts, or commits. This can be done with the
 * {@link #registerLifecycleListener(TransactionLifecycleListener)}.
 * <p/>
 * When the Transaction is reset, the lifecycle tasks are dropped. So the lifecycle task need to be registered again.
 * </dd>
 * <p/>
 * <dt>Blocking</dt>
 * <dd>
 * It is possible to let a transaction block until some state changes has happened; comparable with
 * the waitset/{@link java.util.concurrent.locks.Condition} functionality already provided. For more information see the
 * {@link #registerRetryLatch(org.multiverse.utils.latches.Latch)}.
 * <p/>
 * Because a custom {@link org.multiverse.utils.latches.Latch} implementation can be provided, you have a lot of control on the blocking behavior.
 * But atm it is not possible to get fairness on when the Latch is opened. Policies to customize starvation,
 * lifelocking, deadlocking will be added in the future.
 * </dd>
 * <p/>
 * </dl>
 *
 * @author Peter Veentjer.
 */
public interface Transaction {

    /**
     * Gets the {@link TransactionConfig} that is used by this Transaction.
     *
     * @return the used TransactionConfig.
     */
    TransactionConfig getConfig();

    /**
     * Returns the clock version of the stm when this Transaction started. This version is needed to provide a
     * transaction level read consistent view (so a transaction will always see a stable view of the objects at some
     * point in time).
     * <p/>
     * The value is unspecified once the Transaction is aborted or committed.
     * <p/>
     * Method depends on long as time, and is going to be removed or replaced in the future.
     *
     * @return the version of the stm when this Transaction started.
     */
    long getReadVersion();

    /**
     * Returns the status of this Transaction.
     *
     * @return the status of this Transaction.
     */
    TransactionStatus getStatus();

    /**
     * Commits this Transaction. If the Transaction is:
     * <ol>
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
     * @throws org.multiverse.api.exceptions.DeadTransactionException
     *          if this transaction already is aborted.
     */
    void commit();

    /**
     * Prepares this transaction to be committed. It can lock resources to make sure that no conflicting changes are
     * made after the transaction has been prepared. If the transaction already is prepared, the call is ignored.  If
     * the prepare fails, the transaction automatically is aborted.
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
     * Aborts this Transaction. This means that the changes made in this transaction are not committed. It depends on
     * the implementation if this operation is simple (ditching objects for example), or if changes need to be rolled
     * back. If an exception is thrown while executing the abort, the transaction is still aborted. And example of
     * such a situation is a pre-abort task that fails. So the transaction always is aborted (unless it is committed).
     * <p/>
     * If the Transaction already is aborted, the call is ignored.
     * <p/>
     *
     * @throws org.multiverse.api.exceptions.DeadTransactionException
     *          if this transaction already is committed
     */
    void abort();

    /**
     * Restarts this Transaction. It doesn't matter what the transaction state of the transaction is. This is the
     * preferred way to restart a transaction once a recoverable exception or retry occurred.
     * <p/>
     * If the Transaction is prepared or committed, it will be aborted before it is restarted. If there are
     * TransactionLifecycleListeners that cause problems while executing the pre/post abort notification, the
     * transaction will be aborted and the exception will be propagated.
     */
    void restart();

    /**
     * Registers the retry Latch on this Transaction. This functionality is required for the retry mechanism
     * (so blocking!) and is something different than 'just' restarting. The Latch contains all the 'waiting' logic,
     * so you can do timed and non interruptible timeouts on that structure. A latch can be compared to a
     * {@link java.util.concurrent.Future} because it also pops closed once some event occurs (an interesting write
     * in the case of a Latch).
     *
     * @param latch the Latch to register.
     * @throws NullPointerException          if latch is null.
     * @throws org.multiverse.api.exceptions.NoRetryPossibleException
     *                                       if the retry can't make progress, e.g. because the transaction has not
     *                                       loaded any object.
     * @throws org.multiverse.api.exceptions.DeadTransactionException
     *                                       if this transaction already is committed or aborted.
     * @throws UnsupportedOperationException if the Transaction doesn't support this operation (for example because it
     *                                       doesn't do readtracking).
     */
    void registerRetryLatch(Latch latch);

    /**
     * Registers a TransactionLifecycleListener on this Transaction. It can be used to receive a callback from the
     * transaction if the {@link TransactionStatus} of that transaction. Without this functionality it would not
     * be possible to execute compensating or deferred action once a transaction aborts or commits.
     * <p/>
     * If the execution of one of the listeners fails, the others won't be executed. If one of the listeners fails
     * before the commit/abort, the transaction is aborted (no matter what).
     * <p/>
     * The listeners are executed in the order they are registered.
     * <p/>
     * If the same listener is added multiple times, it will be notified multiple times.
     * <p/>
     * The listener will be executed on the thread that starts the commit/abort.
     * <p/>
     * If the listener is added after the Transaction is prepared, the preCommit event will not be called on the
     * listener.
     * <p/>
     * If the listener accesses the stm after the transaction has been committed or aborted, it could see changes
     * made after that transaction. So all assumptions about state are possibly wrong so one needs to take care of
     * re-validating state if needed.
     * <p/>
     * A good use case of this feature is starting up threads. If you need to start threads, you don't want to start
     * them immediately because eventually the transaction could be aborted.
     *
     * @param listener the TransactionLifecycleListener to registerLifecycleListener
     * @throws NullPointerException if listener is null.
     * @throws org.multiverse.api.exceptions.DeadTransactionException
     *                              if transaction already is aborted or committed.
     */
    void registerLifecycleListener(TransactionLifecycleListener listener);

    //long getTimeout();

    //TimeUnit getTimeoutTimeUnit();

    //void setTimeout(long timeout, TimeUnit timeUnit);
}
