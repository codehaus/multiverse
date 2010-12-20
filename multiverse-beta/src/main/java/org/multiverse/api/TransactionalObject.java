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
     * Gets the {@link Lock} that belongs to this TransactionalObject.
     *
     * @return the Lock
     * @throws UnsupportedOperationException if this operation is not supported.
     */
    Lock getLock();

    /**
     * Does a ensure. What is means is that at the end of the transaction (so deferred)
     * checks if no other transaction has made an update and also guarantees that till the transaction
     * completes no other transaction is able to do an update. Using the deferredEnsure you can
     * coordinate writeskew problem on the reference level. This can safely be called on already
     * ensured/privatized tranlocals (although it doesn't provide any value anymore since the ensure
     * privatize already prevent conflicts).
     * <p/>
     * Unlike the {@link #acquireWriteLock()} which is pessimistic, this is optimistic.
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
    void ensure();

    /**
     * Does a ensure. What is means is that at the end of the transaction (so deferred)
     * checks if no other transaction has made an update and also guarantees that till the transaction
     * completes no other transaction is able to do an update. Using the deferredEnsure you can
     * coordinate writeskew problem on the reference level. This can safely be called on already
     * ensured/privatized tranlocals (although it doesn't provide any value anymore since the ensure
     * privatize already prevent conflicts).
     * <p/>
     * Unlike the {@link #acquireWriteLock(Transaction)} which is pessimistic, this is optimistic.
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
    void ensure(Transaction self);

    /**
     * Returns a debug representation of the TransactionalObject. The data used doesn't have to be consistent,
     * it is a best effort. This method doesn't rely on a running transaction.
     *
     * @return the debug representation of the TransactionalObject.
     */
    String toDebugString();
}
