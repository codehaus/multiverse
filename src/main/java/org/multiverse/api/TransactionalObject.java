package org.multiverse.api;

/**
 * The interface each transactional object needs to implement.
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
     * @throws org.multiverse.api.exceptions.TransactionalExecutionException
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
     * @throws org.multiverse.api.exceptions.TransactionalExecutionException
     *
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    void privatize(Transaction self);

    /**
     * Tries to privatize
     * <p/>
     * This call lifts on the Transaction stored in the ThreadLocalTransaction.
     * <p/>
     * The privatize lock acquired will automatically be acquired for the duration of the transaction and
     * automatically released when the transaction commits or aborts.
     *
     * @return true if the privatization was a success, false otherwise.
     * @throws org.multiverse.api.exceptions.TransactionalExecutionException
     *
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    boolean tryPrivatize();

    /**
     * Tries to privatize this TransactionalObject. This call is reentrant, so it doesn't matter if the
     * TransactionalObject is already privatized by itself. If the TransactionalObject already is ensured
     * by itself, it will be upgraded to a privatize.
     * <p/>
     * Once the privatization is a success, the transaction has exclusive read/write ownership of the
     * TransactionalObject.
     * <p/>
     * If reading the transactional object could cause a read conflict, also false is returned.
     *
     * @param self the Transaction this method lifts on.
     * @return true if the privatization is a success, false otherwise.
     * @throws NullPointerException if tx is null.
     * @throws org.multiverse.api.exceptions.TransactionalExecutionException
     *
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    boolean tryPrivatize(Transaction self);

    /**
     * Checks if the TransactionalObject is privatized. It could be that it is privatized by the active
     * transaction or by another transaction.
     * <p/>
     * The value could be stale as soon as it is returned.
     * <p/>
     * This method lifts on the Transaction stored in the ThreadLocalTransaction.
     *
     * @return true if privatized, false otherwise.
     * @throws org.multiverse.api.exceptions.TransactionalExecutionException
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
     * @throws org.multiverse.api.exceptions.TransactionalExecutionException
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
     * @throws org.multiverse.api.exceptions.TransactionalExecutionException
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
     * @throws org.multiverse.api.exceptions.TransactionalExecutionException
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
     * @throws org.multiverse.api.exceptions.TransactionalExecutionException
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
     * @throws org.multiverse.api.exceptions.TransactionalExecutionException
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
     * @throws org.multiverse.api.exceptions.TransactionalExecutionException
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
     * @throws org.multiverse.api.exceptions.TransactionalExecutionException
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
     * @throws org.multiverse.api.exceptions.TransactionalExecutionException
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
     * @throws org.multiverse.api.exceptions.TransactionalExecutionException
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
     * @throws org.multiverse.api.exceptions.TransactionalExecutionException
     *
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    void ensure(Transaction self);

    /**
     * Tries to ensure this transactional object. Once it is ensured, other transaction still can read,
     * but can't overwrite. If the transactional object already is privatized or ensured, this call
     * is ignored.
     * <p/>
     * If the transaction already read the transactional object, a readconflict check will be done. So once
     * the ensure completes successfully, a transaction has the guarantee that a write will always succeed.
     * <p/>
     * If the ensure fails, and no read was done on this transactional object before, the system behaves
     * as this call never was done.
     * <p/>
     * The lock acquired will automatically be acquired for the duration of the transaction and automatically
     * released when the transaction commits or aborts.
     * <p/>
     * This method lifts on the Transaction stored in the ThreadLocalTransaction.
     *
     * @return true if the ensure was a success, false otherwise.
     * @throws org.multiverse.api.exceptions.TransactionalExecutionException
     *          *
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    boolean tryEnsure();

    /**
     * Tries to ensure this transactional object. Once it is ensured, other transaction still can read,
     * but can't overwrite. If the transactional object already is privatized or ensured, this call
     * is ignored.
     * <p/>
     * If the transaction already read the transactional object, a readconflict check will be done. So once
     * the ensure completes successfully, a transaction has the guarantee that a write will always succeed.
     * <p/>
     * If the ensure fails, and no read was done on this transactional object before, the system behaves
     * as this call never was done.
     * <p/>
     * The lock acquired will automatically be acquired for the duration of the transaction and automatically
     * released when the transaction commits or aborts.
     *
     * @param self the transaction used for doing to ensure.
     * @return true if the ensure was a success, false otherwise.
     * @throws NullPointerException if tx is null
     * @throws org.multiverse.api.exceptions.TransactionalExecutionException
     *
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    boolean tryEnsure(Transaction self);


    /**
     *
     */
    void ensureOptimistic();

    /**
     * @param self
     */
    void ensureOptimistic(Transaction self);
}
