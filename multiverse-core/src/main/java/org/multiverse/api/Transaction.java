package org.multiverse.api;

import org.multiverse.utils.latches.Latch;
import org.multiverse.utils.restartbackoff.RestartBackoffPolicy;

/**
 * All changes on AtomicObjects must be done through a Transaction. The transaction make sure that the changes are: <ol>
 * <li>Atomic: all or nothing gets committed (Failure atomicity)</li> <li>Consistent : </li> <li>Isolated: a transaction
 * is executed isolated from other transactions. Meaning that a transaction won't see changed made by transactions
 * executed concurrently, but it will see changes made by transaction completed before.</li> </ol>
 * <p/>
 * A Transaction is not thread-safe to use (just like a Hibernate Session is not thread-safe to use). It can be handed
 * over from transaction to transaction, but one needs to be really careful with threadlocals. Although the
 * Stm/Transaction implementation don't care about threadlocals, the stuff in front (templates, instrumented code etc)
 * could depend on threadlocals.
 *
 * @author Peter Veentjer.
 */
public interface Transaction {

    /**
     * Returns the family name of this Transaction. Every transaction in principle should have a family name. This
     * information can be used for debugging/logging purposes but also other techniques that rely to know something
     * about similar types of transactions like profiling.
     *
     * @return the familyName. The returned value can be null.
     */
    String getFamilyName();

    /**
     * Returns the clock version of the stm when this Transaction started. This version is needed to provide a
     * transaction level read consistent view (so a transaction will always see a stable view of the objects at some
     * point in time).
     * <p/>
     * The returned version will always be equal or larger than 0.
     * <p/>
     * The value is unspecified once the Transaction is aborted or committed.
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
     *
     * @return the new version if the commit was a success. If the there are no changes, the readVersion if the
     *         transaction is returned. Otherwise the writeVersion of the transaction is returned.
     *
     * @throws org.multiverse.api.exceptions.WriteConflictException
     *
     * @throws org.multiverse.api.exceptions.DeadTransactionException
     *          if this transaction already is aborted.
     */
    long commit();

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
     * Restarts this Transaction by returning a Transaction. This can always be done.
     * <p/>
     * It could be that the same instance is returned that was reinitialized.
     *
     * @return the restarted transaction. Normally this could be the current transaction, but it can also be used to
     *         return a new transaction that is better suited for the job based on the knowledge that was gathered
     *         during execution for example.
     */
    Transaction abortAndReturnRestarted();

    /**
     * Returns the RestartBackoffPolicy for this transaction. Returned value will never be null.
     *
     * @return the RestartBackoffPolicy for this transaction.
     */
    RestartBackoffPolicy getRestartBackoffPolicy();

    /**
     * Aborts and registers the retryList. This functionality is required for the retry mechanism (so blocking!) and is
     * something different than 'just' restarting. The Latch contains all the 'waiting' logic, so you can do timed and
     * non interruptible timeouts. It can be compared to a {@link java.util.concurrent.Future}.
     * <p/>
     * The abort will always succeed even if the registration fails. No need to keep the transaction in a usable state.
     *
     * @param latch the Latch to register.
     * @throws NullPointerException if latch is null.
     * @throws org.multiverse.api.exceptions.NoRetryPossibleException
     *                              if the retry can't make progress, e.g. because the transaction has not loaded any
     *                              object.
     * @throws org.multiverse.api.exceptions.DeadTransactionException
     *                              if this transaction already is committed or aborted.
     */
    void abortAndRegisterRetryLatch(Latch latch);

    /**
     * Starts the 'or' from the 'orelse' block.
     * <p/>
     * The orelse block is needed for the orelse functionality:
     *
     * @throws org.multiverse.api.exceptions.DeadTransactionException
     *                                       if this transaction already is committed or aborted.
     * @throws UnsupportedOperationException if the implementation doesn't support it.
     * @see #endOr()
     * @see #endOrAndStartElse()
     */
    void startOr();

    /**
     * End the 'or' from the orelse block successfully. No rollbacks are done.
     *
     * @throws org.multiverse.api.exceptions.DeadTransactionException
     *                                       if this transaction already is committed or aborted.
     * @throws UnsupportedOperationException if the implementation doesn't support it.
     * @see #startOr()
     * @see #endOrAndStartElse()
     */
    void endOr();

    /**
     * Ends the 'or' from the orelse block, rolls back all changes made within this block, and starts the else block.
     *
     * @throws org.multiverse.api.exceptions.DeadTransactionException
     *                                       if this transaction already is committed or aborted.
     * @throws UnsupportedOperationException if the implementation doesn't support it.
     * @see #endOr()
     * @see #startOr()
     */
    void endOrAndStartElse();

    /**
     * Schedules a task to be executed on the specified life-cycle.
     * <p/>
     * If the execution of one of the tasks fails, the others won't be executed.
     * <p/>
     * There is no guaranteed order of execution of the tasks.
     * <p/>
     * If the same tasks is added multiple times, it could be executed multiple times (no guarantee).
     * <p/>
     * The tasks will be executed on the current thread that causes the commit/abort.
     * <p/>
     * If the task accesses the STM, it could see changes made after the commit of the current transaction. So they will
     * not be running on the same transaction.
     * <p/>
     * A good use case of this feature is starting up threads. If you need to start threads, you don't want to start
     * them immediately because eventually the transaction could be aborted. And another problem is that new transaction
     * started by spawned threads are not able to see the changes already made in the current transaction, because the
     * current transaction hasn't completed yet.
     *
     * @param task         the task to execute after the transaction completes.
     * @param scheduleType
     * @throws NullPointerException if task is null or scheduleType is null.
     * @throws org.multiverse.api.exceptions.DeadTransactionException
     *                              if transaction already is aborted or committed.
     */
    void schedule(Runnable task, ScheduleType scheduleType);
}
