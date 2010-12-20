package org.multiverse.api;

public interface Lock {

    /**
     * Checks if the TransactionalObject is unlocked.
     * <p/>
     * The value could be stale as soon as it is returned.
     *
     * @return true if the TransactionalObject has not been ensured or privatized.
     */
    boolean atomicIsUnlocked();

    /**
     * Privatizes the transactional object. This means that it can't be read or written by another transaction
     * (so the provided transaction will have exclusive ownership of it).
     * <p/>
     * If the transactional object already has been read, a conflict check will be done. So once privatized,
     * you have the guarantee that a transaction can commit on this transactional object (although it still
     * can fail on other transactional objects).
     * <p/>
     * The lock acquired will automatically be acquired for the duration of the transaction and automatically
     * released when the transaction commits or aborts.
     *
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    void acquireCommitLock();

    /**
     * Privatizes the transactional object using the provided transaction. This means that it can't be read
     * or written by another transaction (so the provided transaction will have exclusive ownership of it).
     * <p/>
     * If the transactional object already has been read, a conflict check will be done. So once privatized,
     * you have the guarantee that a transaction can commit on this transactional object (although it still
     * can fail on other transactional objects).
     * <p/>
     * The privatize lock acquired will automatically be acquired for the duration of the transaction and
     * automatically released when the transaction commits or aborts.
     *
     * @param self the transaction used to privatize the transactional object.
     * @throws NullPointerException if tx is null
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    void acquireCommitLock(Transaction self);

    /**
     * Checks if the TransactionalObject is privatized. It could be that it is privatized by the active
     * transaction or by another transaction.
     * <p/>
     * The value could be stale as soon as it is returned.
     * <p/>
     * This method lifts on the Transaction stored in the ThreadLocalTransaction.
     *
     * @return true if privatized, false otherwise.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    boolean atomicIsLockedForCommit();

    /**
     * Checks if the TransactionalObject is privatized by another transaction.
     * <p/>
     * The value could be stale as soon as it is returned.
     * <p/>
     * This call lifts on the Transaction stored in the ThreadLocalTransaction.
     *
     * @return true if the TransactionalObject has been privatized by another transaction than the
     *         active transaction.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    boolean isLockedForCommitByOther();

    /**
     * Checks if the TransactionalObject is privatized by another transaction than the provided transaction.
     * If the transaction is privatized by itself, it will also return false.
     * <p/>
     * The value could be stale as soon as it is returned (unless is it privatized by itself).
     *
     * @param self the transaction to compare with.
     * @return true if the TransactionalObject is privatized by another transaction.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *
     * @throws NullPointerException if tx is null.
     */
    boolean isLockedForCommitByOther(Transaction self);

    /**
     * Checks if the TransactionalObject is privatized by the active transaction.
     * <p/>
     * The value could be stale as soon as it is returned.
     * <p/>
     * This call lifts on the Transaction stored in the ThreadLocalTransaction.
     *
     * @return true if the TransactionalObject is privatized by the active transaction.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    boolean isLockedForCommitBySelf();

    /**
     * Checks if the TransactionalObject is privatized by the provided transaction.
     * <p/>
     * The value could be stale as soon as it is returned.
     *
     * @param self the transaction to check with.
     * @return true if the TransactionalObject it privatized by the provided transaction, false otherwise.
     * @throws NullPointerException if tx is null.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    boolean isLockedForCommitBySelf(Transaction self);

    /**
     * Checks if the TransactionObject is ensured by a transaction.
     * <p/>
     * The value could be stale as soon as it is returned.
     *
     * @return true if the transaction is ensured, false otherwise.
     */
    boolean atomicIsLockedForUpdate();

    /**
     * Checks if the TransactionalObject is ensured by the active transaction.
     * <p/>
     * The value could be stale as soon as it is returned.
     * <p/>
     * This call lifts on the Transaction stored in the ThreadLocalTransaction.
     *
     * @return true if ensured by itself, false otherwise.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    boolean isLockedForWriteBySelf();

    /**
     * Checks if the TransactionalObject is ensured by the provided transaction.
     * <p/>
     * The value could be stale as soon as it is returned.
     *
     * @param self the transaction to check with.
     * @return true if ensured by self, false otherwise.
     * @throws NullPointerException if tx is null.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    boolean isLockedForWriteBySelf(Transaction self);

    /**
     * Checks if the TransactionalObject is ensured by another transaction than the active transaction.
     * <p/>
     * The value could be stale as soon as it is returned.
     * <p/>
     * This call lifts on the Transaction stored in the ThreadLocalTransaction.
     *
     * @return true if the TransactionalObject is ensured by another, false otherwise.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    boolean isLockedForWriteByOther();

    /**
     * Checks if the TransactionalObject is ensured by another transaction than the provided transaction.
     * If the TransactionalObject is ensured by the provided transaction, false will be returned.
     * <p/>
     * The value could be stale as soon as it is returned.
     *
     * @param self the Transaction to check with.
     * @return true if privatized by another Transaction, false otherwise.
     * @throws NullPointerException if tx is null.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    boolean isLockedForWriteByOther(Transaction self);

    /**
     * Ensures that when this ref is read in a transaction, no other transaction is able to commit to this
     * reference or do an ensure. Once it is ensured, it is guaranteed to commit (unless the transaction
     * aborts otherwise). So other transactions still can read the TransactionalObject.
     * <p/>
     * This call is pessimistic.The lock acquired will automatically be acquired for the duration of the transaction
     * and automatically released when the transaction commits or aborts.
     * <p/>
     * This call lifts on the Transaction stored in the ThreadLocalTransaction.
     *
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    void acquireWriteLock();

    /**
     * Ensures that when this ref is read in a transaction, no other transaction is able to write to this
     * reference. Once it is ensured, it is guaranteed to commit (unless the transaction aborts otherwise).
     * <p/>
     * The lock acquired will automatically be acquired for the duration of the transaction and automatically
     * released when the transaction commits or aborts.
     * <p/>
     * This call lifts on the Transaction stored in the ThreadLocalTransaction.
     * <p/>
     *
     * @param self the Transaction used for this operation.
     * @throws NullPointerException if tx is null.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    void acquireWriteLock(Transaction self);

}
