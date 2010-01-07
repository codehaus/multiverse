package org.multiverse.api;

/**
 * All changes on transaction objects must be done through a Transaction. The transaction make sure that the changes
 * are: <ol> <li>Atomic: all or nothing gets committed (Failure atomicity)</li> <li>Consistent : </li> <li>Isolated: a
 * transaction is executed isolated from other transactions. Meaning that a transaction won't see changed made by
 * transactions executed concurrently, but it will see changes made by transaction completed before.</li> </ol>
 * <p/>
 * <h3>Thread-safety</h3> A Transaction is not thread-safe to use (just like a Hibernate Session is not thread-safe to
 * use). It can be handed over from thread to thread, but one needs to be really careful with threadlocals. Although the
 * Stm/Transaction implementation don't care about threadlocals, the stuff in front (templates, instrumented code etc)
 * could depend on threadlocals.
 * <p/>
 * <h3>Listen to lifecycles</h3> It is possible to listen to a Transaction when it aborts, or commits. This can be done
 * with the {@link #register(TransactionLifecycleListener)}
 * <p/>
 * When the Transaction is reset, the lifecycle tasks are dropped. So the lifecycle task need to be registered again.
 * <h3>Blocking</h3> It is possible to let a transaction block until some state changes has happened; comparable with
 * the waitset/{@link java.util.concurrent.locks.Condition} functionality already provided. For more information see the
 * {@link #registerRetryLatch(Latch)}.
 * <p/>
 * Because a custom {@link Latch} implementation can be provided, you have a lot of control on the blocking behavior.
 * But atm it is not possible to get fairness on when the Latch is opened. Policies to customize starvation,
 * lifelocking, deadlocking will be added in the future.
 *
 * @author Peter Veentjer.
 */
public interface Transaction {

    /**
     * Gets the TransactionConfig that is used by this Transaction.
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
     * TODO: Method depends on long as time, not a good thing
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
     * Commits this Transaction. If the Transaction already is committed, the call is ignored.
     * <p/>
     * Transaction will be aborted if the commit does not succeed.
     * <p/>
     *
     * @throws org.multiverse.api.exceptions.CommitFailureException
     *          if the commit failed. Check the class hierarchy of the CommitFailureException for more information.
     * @throws org.multiverse.api.exceptions.DeadTransactionException
     *          if this transaction already is aborted.
     */
    void commit();

    /**
     * Prepares this transaction to be committed. It can lock resources to make sure that no conflicting changes are
     * made after the transaction has been prepared.
     * <p/>
     * It the prepare doesn't succeed, it must release all sources acquired because no guarantee is made that the abort
     * is called.
     * <p/>
     * todo: nothing is said over the exception when the prepare didn't succeed.
     *
     * @throws org.multiverse.api.exceptions.DeadTransactionException
     *          if the transaction already is committed or aborted.
     * @throws org.multiverse.api.exceptions.PreparedTransactionException
     *          if the transaction already is prepared
     */
    void prepare();

    /**
     * Aborts this Transaction. This means that the changes made in this transaction are not committed. It depends on
     * the implementation if this operation is simple (ditching objects for example), or if changes need to be rolled
     * back.
     * <p/>
     * If the Transaction already is aborted, the call is ignored.
     * <p/>
     * It is important that the abort never fails.
     *
     * @throws org.multiverse.api.exceptions.DeadTransactionException
     *          if this transaction already is committed
     */
    void abort();

    /**
     * Restarts this Transaction. It doesn't matter what the transaction state of the transaction is. This is the
     * preferred way to restart a transaction once a recoverable exception or retry occurred.
     */
    void restart();

    /**
     * Registers the retry Latch. This functionality is required for the retry mechanism (so blocking!) and is something
     * different than 'just' restarting. The Latch contains all the 'waiting' logic, so you can do timed and non
     * interruptible timeouts. It can be compared to a {@link java.util.concurrent.Future}.
     * <p/>
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
     * Registers a TransactionLifecycleListener to this Transaction.
     * <p/>
     * If the execution of one of the listeners fails, the others won't be executed. If it listeners fails before the
     * precommit, the transaction is aborted (no matter what).
     * <p/>
     * There is no guaranteed order of execution of the listeners
     * <p/>
     * If the same listener is added multiple times, it could be notified multiple times (no guarantee).
     * <p/>
     * The listener will be executed on the current thread that causes the commit/abort.
     * <p/>
     * If the listener accesses the STM, it could see changes made after the commit/abort of the current transaction.
     * This is because it is not running in the same transaction because the current transaction already is committed or
     * aborted.
     * <p/>
     * A good use case of this feature is starting up threads. If you need to start threads, you don't want to start
     * them immediately because eventually the transaction could be aborted. And another problem is that new transaction
     * started by spawned threads are not able to see the changes already made in the current transaction, because the
     * current transaction hasn't completed yet.
     *
     * @param listener the TransactionLifecycleListener to register
     * @throws NullPointerException if listener is null.
     * @throws org.multiverse.api.exceptions.DeadTransactionException
     *                              if transaction already is aborted or committed.
     * @throws org.multiverse.api.exceptions.PreparedTransactionException
     *                              if the transaction is prepared.
     */
    void register(TransactionLifecycleListener listener);
}
