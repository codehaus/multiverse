package org.multiverse.api;

/**
 * The interface each transactional object needs to implement.
 * <p/>
 * <h1>Ensure</h1>
 * <p/>
 * <h1>Privatize</h1>
 *
 * @author Peter Veentjer.
 */
public interface TransactionalObject {

    /**
     * Returns the Stm this TransactionalObject is part of. Once a TransactionalObject is created using some
     * STM, it will never become part of another STM.
     *
     * @return the Stm this TransactionalObject is part of. Returned value will never be null.
     */
    Stm getStm();

    /**
     * Checks if the TransactionalObject has not been ensured or privatized.
     * <p/>
     * The value could be stale as soon as it is returned.
     *
     * @return true if the TransactionalObject has not been ensured or privatized.
     */
    boolean atomicIsFree();

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
    void privatize();

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
    void privatize(Transaction self);

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
    boolean atomicIsPrivatized();

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
    boolean isPrivatizedByOther();

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
    boolean isPrivatizedByOther(Transaction self);

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
    boolean isPrivatizedBySelf();

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
    boolean isPrivatizedBySelf(Transaction self);

    /**
     * Checks if the TransactionObject is ensured by a transaction.
     * <p/>
     * The value could be stale as soon as it is returned.
     *
     * @return true if the transaction is ensured, false otherwise.
     */
    boolean atomicIsEnsured();

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
    boolean isEnsuredBySelf();

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
    boolean isEnsuredBySelf(Transaction self);

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
    boolean isEnsuredByOther();

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
    boolean isEnsuredByOther(Transaction self);

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
    void ensure();

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
    void ensure(Transaction self);

    /**
     * Does a deferred ensure. What is means is that at the end of the transaction (so deferred)
     * checks if no other transaction has made an update and also guarantees that till the transaction
     * completes no other transaction is able to do an update. Using the deferredEnsure you can
     * coordinate writeskew problem on the reference level. This can safely be called on already
     * ensured/privatized tranlocals (although it doesn't provide any value anymore since the ensure
     * privatize already prevent conflicts).
     * <p/>
     * Unlike the {@link #ensure()} which is pessimistic, this is optimistic.
     * <p/>
     * This method doesn't provide any value if the transaction is readonly.
     * <p/>
     * This call lifts on the Transaction stored in the ThreadLocalTransaction.
     *
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    void deferredEnsure();

    /**
     * Does a deferred ensure. What is means is that at the end of the transaction (so deferred)
     * checks if no other transaction has made an update and also guarantees that till the transaction
     * completes no other transaction is able to do an update. Using the deferredEnsure you can
     * coordinate writeskew problem on the reference level. This can safely be called on already
     * ensured/privatized tranlocals (although it doesn't provide any value anymore since the ensure
     * privatize already prevent conflicts).
     * <p/>
     * Unlike the {@link #ensure(Transaction)} which is pessimistic, this is optimistic.
     * <p/>
     * This method doesn't provide any value if the transaction is readonly.
     * <p/>
     *
     * @param self the Transaction this call lifts on.
     * @throws NullPointerException if self is null.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    void deferredEnsure(Transaction self);

    /**
     * Returns a debug representation of the TransactionalObject. The data used doesn't have to be consistent,
     * it is a best effort. This method doesn't rely on a running transaction.
     *
     * @return the debug representation of the TransactionalObject.
     */
    String toDebugString();
}