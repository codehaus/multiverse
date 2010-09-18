package org.multiverse.api;

/**
 * The interface each transactional object needs to implement.
 *
 * @author Peter Veentjer.
 */
public interface TransactionalObject {

    /**
     * Checks if the TransactionalObject has not been ensured or privatized.
     * <p/>
     * The value could be stale as soon as it is returned.
     *
     * @return true if the TransactionalObject has not been ensured or privatized.
     */
    boolean isFree();

    /**
     * Checks if the TransactionalObject is privatized. It could be that it is privatized by the active
     * transaction or by another transaction.
     *
     * The value could be stale as soon as it is returned.
     *
     * @return true if privatized, false otherwise.
     */
    boolean isPrivatized();

    /**
     * Checks if the TransactionalObject is privatized by another transaction.
     *
     * The value could be stale as soon as it is returned.
     *
     * @return true if the TransactionalObject has been privatized by another transaction than the
     *         active transaction.
     * @throws org.multiverse.api.exceptions.NoTransactionFoundException
     *          if no alive transaction is found.
     */
    boolean isPrivatizedByOther();

    /**
     * Checks if the TransactionalObject is privatized by another transaction than the provided transaction.
     *
     * @param tx the transaction to compare with.
     * @return true if the TransactionalObject is privatized by another transaction.
     * @throws NullPointerException if tx is null.
     */
    boolean isPrivatizedByOther(Transaction tx);

    /**
     * Checks if the TransactionalObject is privatized by the active transaction.
     *
     * The value could be stale as soon as it is returned.
     *
     * @return
     * @throws org.multiverse.api.exceptions.NoTransactionFoundException if no alive transaction is found.
     */
    boolean isPrivatizedBySelf();

    boolean isPrivatizedBySelf(Transaction tx);

    boolean isEnsured();

    boolean isEnsuredBySelf();

    boolean isEnsuredBySelf(Transaction tx);

    boolean isEnsuredByOther();

    boolean isEnsuredByOther(Transaction tx);

    /**
     * Returns the Stm this TransactionalObject is part of.
     *
     * @return the Stm this TransactionalObject is part of.
     */
    Stm getStm();

    /**
     * Ensures that when this ref is read in a transaction, no other transaction is able to write to this
     * reference. Once it is ensured, it is guaranteed to commit (unless the transaction aborts otherwise).
     * <p/>
     * This call expects a running transaction.
     * <p/>
     * This call is pessimistic.
     * <p/>
     * The lock acquired will automatically be acquired for the duration of the transaction and automatically
     * released when the transaction commits or aborts.
     *
     * @throws IllegalStateException
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    void ensure();

    /**
     * Ensures that when this ref is read in a transaction, no other transaction is able to write to this
     * reference. Once it is ensured, it is guaranteed to commit (unless the transaction aborts otherwise).
     * <p/>
     * This call expects a running transaction.
     * <p/>
     * The lock acquired will automatically be acquired for the duration of the transaction and automatically
     * released when the transaction commits or aborts.
     *
     * @param tx the Transaction used for this operation.
     * @throws NullPointerException if tx is null.
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *                              if the transaction is
     *                              not in the correct state for this operation.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    void ensure(Transaction tx);

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
     * @return true if the ensure was a success, false otherwise.
     * @throws org.multiverse.api.exceptions.NoTransactionFoundException
     *          if no transaction was found
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *          if the transaction was not
     *          in the correct state for this operation.
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
     * @param tx the transaction used for doing to ensure.
     * @return true if the ensure was a success, false otherwise.
     * @throws NullPointerException if tx is null
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *                              if the transaction wasn't in
     *                              the correct state for this operation.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    boolean tryEnsure(Transaction tx);

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
     * @throws org.multiverse.api.exceptions.NoTransactionFoundException
     *          if no transaction is found.
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
     * The lock acquired will automatically be acquired for the duration of the transaction and automatically
     * released when the transaction commits or aborts.
     *
     * @param tx the transaction used to privatize the transactional object.
     * @throws NullPointerException if tx is null
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *                              if the transaction isn't in the
     *                              correct state for this operation.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    void privatize(Transaction tx);

    /**
     * Tries to privatize
     * <p/>
     * The lock acquired will automatically be acquired for the duration of the transaction and automatically
     * released when the transaction commits or aborts.
     *
     * @return true if the privatization was a success, false otherwise.
     * @throws org.multiverse.api.exceptions.NoTransactionFoundException
     *          if no transaction was found
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *          if the transaction found was not
     *          in the correct state for this operation.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    boolean tryPrivatize();

    /**
     * @param tx
     * @return
     */
    boolean tryPrivatize(Transaction tx);

    /**
     *
     */
    void ensureOptimistic();

    /**
     * @param tx
     */
    void ensureOptimistic(Transaction tx);
}
