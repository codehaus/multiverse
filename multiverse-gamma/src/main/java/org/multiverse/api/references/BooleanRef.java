package org.multiverse.api.references;

import org.multiverse.api.*;
import org.multiverse.api.functions.*;
import org.multiverse.api.predicates.*;

/**
 * A Transactional Reference comparable to the <a href="http://clojure.org/refs">Clojure Ref</a>.
 * If a method is prefixed with atomic, the call will always run under its own transaction, no
 * matter if there already is a transaction available (so the propagation level is {@link PropagationLevel#RequiresNew}).
 * For the other methods, always an transaction needs to be available, else you will get the
 * {@link org.multiverse.api.exceptions.TransactionMandatoryException}.
 *
 * <h3>ControlFlowError</h3>
 *
 * <p>All non atomic methods are able to throw a (subclass) of the {@link org.multiverse.api.exceptions.ControlFlowError}. This error should
 * not be caught, it is task of the {@link AtomicBlock} to deal with.
 * 
 * <h3>TransactionExecutionException</h3>
 *
 * <p>Most of the methods can throw a {@link org.multiverse.api.exceptions.TransactionExecutionException}.
 * This exception can be caught, but in most cases you want to figure out what the cause is (e.g. because
 * there are too many retries) and solve that problem.
 *
 * <h3>Threadsafe</h3>
 *
 * <p>All methods are threadsafe.
 *
 * @author Peter Veentjer.
 */
public interface BooleanRef extends TransactionalObject {

    /**
     * Gets the value using the provided transaction.
     *
     * @return the current value.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *                  if something failed while using the transaction. The transaction is guaranteed to have been aborted.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *                  if the Stm needs to control the flow in a different way than normal returns of exceptions. The transaction
     *                  is guaranteed to have been aborted.
     * @see #atomicGet()
     */
    boolean get();

     /**
     * Gets the value and applies the lock. If the current lockMode already is higher than the provided lockMode
     * the Lock is not upgraded to a higher value.
     *
     * <p>This call lifts on the {@link org.multiverse.api.Transaction} stored in the {@link org.multiverse.api.ThreadLocalTransaction}.
     *
     * @param lockMode the LockMode applied.
     * @return the current value.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *                  if something failed while using the transaction. The transaction is guaranteed to have been aborted.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *                  if the Stm needs to control the flow in a different way than normal returns of exceptions. The transaction
     *                  is guaranteed to have been aborted.
     * @see #atomicGet()
     */
    boolean getAndLock(LockMode lockMode);

    /**
     * Gets the value using the provided transaction.
     *
     * @param tx the {@link Transaction} used for this operation.
     * @return the value stored in the ref.
     * @throws NullPointerException if tx is null.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *                  if something failed while using the transaction. The transaction is guaranteed to have been aborted.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *                  if the Stm needs to control the flow in a different way than normal returns of exceptions. The transaction
     *                  is guaranteed to have been aborted.
     */
    boolean get(Transaction tx);

    /**
     * Gets the value using the provided transaction and acquired the lock with the specified LockMode.
     *
     * @param tx the {@link Transaction} used for this operation.
     * @param lockMode the LockMode used
     * @return the value stored in the ref.
     * @throws NullPointerException if tx is null or if lockMode is null. If LockMode is null and a running transaction is available
     *                              it will be aborted.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *                  if something failed while using the transaction. The transaction is guaranteed to have been aborted.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *                  if the Stm needs to control the flow in a different way than normal returns of exceptions. The transaction
     *                  is guaranteed to have been aborted.
     */
    boolean getAndLock(Transaction tx, LockMode lockMode);

    /**
     * Sets the new value.
     *
     * <p>This call lifts on the {@link org.multiverse.api.Transaction} stored in the {@link org.multiverse.api.ThreadLocalTransaction}.
     *
     * @param value the new value.
     * @return the new value.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *                  if something failed while using the transaction. The transaction is guaranteed to have been aborted.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *                  if the Stm needs to control the flow in a different way than normal returns of exceptions. The transaction
     *                  is guaranteed to have been aborted.
     */
    boolean set(boolean value);

    /**
     * Sets the new value and applies the lock.
     *
     * <p>This call lifts on the {@link org.multiverse.api.Transaction} stored in the {@link org.multiverse.api.ThreadLocalTransaction}.
     *
     * @param value the new value.
     * @param lockMode the used LockMode.
     * @return the new value.
     * @throws NullPointerException if lockMode is null (if the transaction is alive, it will also be aborted.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *                  if something failed while using the transaction. The transaction is guaranteed to have been aborted.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *                  if the Stm needs to control the flow in a different way than normal returns of exceptions. The transaction
     *                  is guaranteed to have been aborted.
     */
    boolean setAndLock(boolean value, LockMode lockMode);

   /**
    * Sets the new value using the provided transaction.
    *
    * @param tx the {@link Transaction} used for this operation.
    * @param value the new value
    * @return the old value
    * @throws NullPointerException if tx is null.
    * @throws org.multiverse.api.exceptions.TransactionExecutionException
    *                  if something failed while using the transaction. The transaction is guaranteed to have been aborted.
    * @throws org.multiverse.api.exceptions.ControlFlowError
    *                  if the Stm needs to control the flow in a different way than normal returns of exceptions. The transaction
    *                  is guaranteed to have been aborted.
    */
    boolean set(Transaction tx, boolean value);

    /**
    * Sets the new value using the provided transaction.
    *
    * @param tx the {@link Transaction} used for this operation.
    * @param value the new value
    * @param lockMode the lockMode used.
    * @return the old value
    * @throws NullPointerException if tx is null or lockMode is null. If the lockMode is null and the transaction
    *                              is alive, it will be aborted.
    * @throws org.multiverse.api.exceptions.TransactionExecutionException
    *                  if something failed while using the transaction. The transaction is guaranteed to have been aborted.
    * @throws org.multiverse.api.exceptions.ControlFlowError
    *                  if the Stm needs to control the flow in a different way than normal returns of exceptions. The transaction
    *                  is guaranteed to have been aborted.
    */
    boolean setAndLock(Transaction tx, boolean value, LockMode lockMode);

    /**
     * Sets the value the value and returns the new value.
     *
     * <p>This call lifts on the {@link org.multiverse.api.Transaction} stored in the {@link org.multiverse.api.ThreadLocalTransaction}.
     *
     * @param value the new value.
     * @return the old value.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *                  if something failed while using the transaction. The transaction is guaranteed to have been aborted.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *                  if the Stm needs to control the flow in a different way than normal returns of exceptions. The transaction
     *                  is guaranteed to have been aborted.
     */
    boolean getAndSet(boolean value);

    /**
     * Sets the value, acquired the Lock with the specified Lockmode and returns the previous value.
     *
     * <p>This call lifts on the {@link org.multiverse.api.Transaction} stored in the {@link org.multiverse.api.ThreadLocalTransaction}.
     *
     * @param value the new value.
     * @return the old value.
     * @param lockMode the LockMode used.
     * @throws NullPointerException if LockMode is null. If a running transaction is available, it will be aborted.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *                  if something failed while using the transaction. The transaction is guaranteed to have been aborted.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *                  if the Stm needs to control the flow in a different way than normal returns of exceptions. The transaction
     *                  is guaranteed to have been aborted.
     */
    boolean getAndSetAndLock(boolean value, LockMode lockMode);

    /**
     * Sets the value using the provided transaction.
     *
     * @param value the new value.
     * @param tx the {@link Transaction} used for this operation.
     * @return the old value.
     * @throws NullPointerException if tx is null.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *                  if something failed while using the transaction. The transaction is guaranteed to have been aborted.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *                  if the Stm needs to control the flow in a different way than normal returns of exceptions. The transaction
     *                  is guaranteed to have been aborted.
     */
    boolean getAndSet(Transaction tx, boolean value);

    /**
     * Sets the value and acquired the Lock with the provided LockMode.
     *
     * <p>This call lifts on the {@link org.multiverse.api.Transaction} stored in the {@link org.multiverse.api.ThreadLocalTransaction}.
     *
     * @param value the new value.
     * @param tx the {@link Transaction} used for this operation.
     * @param lockMode the LockMode used.
     * @return the old value.
     * @throws NullPointerException if tx or LockMode is null. If the transaction is running, and the LockMode is null,
     *                              it will be aborted.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *                  if something failed while using the transaction. The transaction is guaranteed to have been aborted.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *                  if the Stm needs to control the flow in a different way than normal returns of exceptions. The transaction
     *                  is guaranteed to have been aborted.
     */
    boolean getAndSetAndLock(Transaction tx, boolean value, LockMode lockMode);

    /**
     * Atomically gets the value. The value could be stale as soon as it is returned. This
     * method doesn't care about any running transactions. It could be that this call fails
     * e.g. when a ref is locked. If you don't care about correct orderings, see the
     * {@link #atomicWeakGet()}.
     *
     * @return the current value.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     */
    boolean atomicGet();

    /**
     * Atomically gets the value without providing any ordering guarantees. This method is extremely
     * cheap and will never fail. So even if the ref is privatized, this call will still complete.
     *
     * <p>It is the best method to call if you just want to get the current value stored.
     *
     * @return the value.
     */
    boolean atomicWeakGet();

    /**
     * Atomically sets the value and returns the new value. This method doesn't care about any
     * running transactions.
     *
     * @param newValue the new value.
     * @return the new value.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     */
    boolean atomicSet(boolean newValue);

    /**
     * Atomically sets the value and returns the previous value. This method doesn't care about
     * any running transactions.
     *
     * @param newValue the new value.
     * @return the old value.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     */
    boolean atomicGetAndSet(boolean newValue);

     /**
     * Applies the function on the ref in a commuting manner. So if there are no dependencies, the function
     * will commute. If somehow there already is a dependency or a dependency is formed on the result of
     * the commuting function, the function will not commute and will be exactly the same as an alter.
     *
     * <p>This is different than the behavior in Clojure where the commute will be re-applied at the end
     * of the transaction, even though some dependency is introduced, which can lead to inconsistencies.
     *
     * <p>This call lifts on the {@link org.multiverse.api.Transaction} stored in the {@link org.multiverse.api.ThreadLocalTransaction}.
     *
     * @param function the function to apply to this reference.
     * @throws NullPointerException if function is null.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *                  if something failed while using the transaction. The transaction is guaranteed to have been aborted.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *                  if the Stm needs to control the flow in a different way than normal returns of exceptions. The transaction
     *                  is guaranteed to have been aborted.
     */
    void commute(BooleanFunction function);

    /**
     * Applies the function on the ref in a commuting manner. So if there are no dependencies, the function
     * will commute. If somehow there already is a dependency or a dependency is formed on the result of
     * the commuting function, the function will not commute and will be exactly the same as an alter.
     *
     * <p>This is different than the behavior in Clojure where the commute will be re-applied at the end
     * of the transaction, even though some dependency is introduced, which can lead to inconsistencies.
     *
     * <p>This call lifts on the {@link org.multiverse.api.Transaction} stored in the {@link org.multiverse.api.ThreadLocalTransaction}.
     *
     * @param tx the {@link Transaction} used for this operation.
     * @param function the function to apply to this reference.
     * @throws NullPointerException  if function is null. If there is an active transaction, it will be aborted.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *                  if something failed while using the transaction. The transaction is guaranteed to have been aborted.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *                  if the Stm needs to control the flow in a different way than normal returns of exceptions. The transaction
     *                  is guaranteed to have been aborted.
     */
    void commute(Transaction tx,BooleanFunction function);

    /**
     * Atomically applies the function to the current value in this ref and returns the new value. This method doesn't care about
     * any running transactions.
     *
     * @param function the Function used
     * @return the new value.
     * @throws NullPointerException if function is null.
     */
    boolean atomicAlterAndGet(BooleanFunction function);

    /**
     * Alters the value stored in this Ref using the provided function and returns the result.
     *
     * <p>This call lifts on the {@link org.multiverse.api.Transaction} stored in the {@link org.multiverse.api.ThreadLocalTransaction}.
     *
     * @param function the function that alters the value stored in this Ref.
     * @return the new value.
     * @throws NullPointerException if function is null. The Transaction will also be aborted.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *                  if something failed while using the transaction. The transaction is guaranteed to have been aborted.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *                  if the Stm needs to control the flow in a different way than normal returns of exceptions. The transaction
     *                  is guaranteed to have been aborted.
     */
    boolean alterAndGet(BooleanFunction function);

    /**
     * Alters the value stored in this Ref using the provided function and lifting on the provided transaction.
     *
     * @param function the function that alters the value stored in this Ref.
     * @param tx the {@link Transaction} used for this operation.
     * @return the new value.
     * @throws NullPointerException if function or transaction is null.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *                  if something failed while using the transaction. The transaction is guaranteed to have been aborted.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *                  if the Stm needs to control the flow in a different way than normal returns of exceptions. The transaction
     *                  is guaranteed to have been aborted.
     */
    boolean alterAndGet(Transaction tx,BooleanFunction function);

    /**
     * Atomically applies the function to alter the value stored in this ref and returns the old value. This method doesn't care about
     * any running transactions.
     *
     * @param function the Function used
     * @return the old value.
     * @throws NullPointerException if function is null.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     */
    boolean atomicGetAndAlter(BooleanFunction function);

    /**
     * Alters the value stored in this Ref using the provided function amd returns the old value.
     *
     * <p>This call lifts on the {@link org.multiverse.api.Transaction} stored in the {@link org.multiverse.api.ThreadLocalTransaction}.
     *
     * @param function the function that alters the value stored in this Ref.
     * @return the old value.
     * @throws NullPointerException if function is null. The transaction will be aborted as well.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *                  if something failed while using the transaction. The transaction is guaranteed to have been aborted.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *                  if the Stm needs to control the flow in a different way than normal returns of exceptions. The transaction
     *                  is guaranteed to have been aborted.
     */
    boolean getAndAlter(BooleanFunction function);

    /**
     * Alters the value stored in this Ref using the function and returns the old value, using the provided transaction.
     *
     * @param function the function that alters the value stored in this Ref.
     * @param tx the {@link Transaction} used for this operation.
     * @return the old value
     * @throws NullPointerException if function or transaction is null. The transaction will be aborted as well.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *                  if something failed while using the transaction. The transaction is guaranteed to have been aborted.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *                  if the Stm needs to control the flow in a different way than normal returns of exceptions. The transaction
     *                  is guaranteed to have been aborted.
     */
    boolean getAndAlter(Transaction tx, BooleanFunction function);

    /**
     * Executes a compare and set atomically. This method doesn't care about any running transactions.
     *
     * @param expectedValue the expected value.
     * @param newValue the new value.
     * @return true if the compareAndSwap was a success, false otherwise.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     */
    boolean atomicCompareAndSet(boolean expectedValue, boolean newValue);

    /**
     * Awaits for the value to become the given value. If the value already has the
     * the specified value, the call continues, else a retry is done.
     *
     * <p>This call lifts on the {@link org.multiverse.api.Transaction} stored in the {@link org.multiverse.api.ThreadLocalTransaction}.
     *
     * @param value the value to wait for.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *                  if something failed while using the transaction. The transaction is guaranteed to have been aborted.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *                  if the Stm needs to control the flow in a different way than normal returns of exceptions. The transaction
     *                  is guaranteed to have been aborted.
     */
    void await(boolean value);

    /**
     * Awaits for the reference to become the given value. If the value already has the
     * the specified value, the call continues, else a retry is done.
     *
     * @param tx the {@link Transaction} used for this operation.
     * @param value the value to wait for.
     * @throws NullPointerException if tx is null.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *                  if something failed while using the transaction. The transaction is guaranteed to have been aborted.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *                  if the Stm needs to control the flow in a different way than normal returns of exceptions. The transaction
     *                  is guaranteed to have been aborted.
     */
    void await(Transaction tx,boolean value);

    /**
     * Awaits until the predicate holds.  If the value already evaluates to true, the call continues
     * else a retry is done. If the predicate throws an exception, the transaction is aborted and the
     * throwable is propagated.
     *
     * <p>This call lifts on the {@link org.multiverse.api.Transaction} stored in the {@link org.multiverse.api.ThreadLocalTransaction}.
     *
     * @param predicate the predicate to evaluate.
     * @throws NullPointerException if predicate is null. When there is a non dead transaction,
     *                              it will be aborted.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *                  if something failed while using the transaction. The transaction is guaranteed to have been aborted.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *                  if the Stm needs to control the flow in a different way than normal returns of exceptions. The transaction
     *                  is guaranteed to have been aborted.
     */
    void await(BooleanPredicate predicate);

    /**
     * Awaits until the predicate holds using the provided transaction.  If the value already evaluates to true, the call continues
     * else a retry is done. If the predicate throws an exception, the transaction is aborted and the
     * throwable is propagated.
     *
     * @param tx the {@link Transaction} used for this operation.
     * @param predicate the predicate to evaluate.
     * @throws NullPointerException if predicate is null or tx is null. When there is a non dead transaction,
     *                              it will be aborted.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *                  if something failed while using the transaction. The transaction is guaranteed to have been aborted.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *                  if the Stm needs to control the flow in a different way than normal returns of exceptions. The transaction
     *                  is guaranteed to have been aborted.
     */
    void await(Transaction tx, BooleanPredicate predicate);
}
