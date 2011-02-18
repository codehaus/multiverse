package org.multiverse.api.references;

import org.multiverse.api.*;
import org.multiverse.api.functions.*;
import org.multiverse.api.predicates.*;

/**
 * A Transactional Reference comparable to the <a href="http://clojure.org/refs">Clojure Ref</a>.
 * If a method is prefixed with atomic, the call will always run under its own transaction, no
 * matter if there already is a transaction available (so the propagation level is Requires New).
 * For the other methods, always an transaction needs to be available, else you will get the
 * {@link org.multiverse.api.exceptions.TransactionMandatoryException}.
 *
 * <h1>ControlFlowError</h1>
 * All non atomic methods are able to throw a (subclass) of the {@link org.multiverse.api.exceptions.ControlFlowError}. This error should
 * not be caught, it is task of the {@link AtomicBlock} to deal with.
 * 
 * <h1>TransactionalExecutionException</h1>
 * Most of the methods can throw a {@link org.multiverse.api.exceptions.TransactionExecutionException}.
 * This exception can be caught, but in most cases you want to figure out what the cause is (e.g. because
 * there are too many retries) and solve that problem.
 *
 * <h1>Threadsafe</h1>
 * All methods are threadsafe.
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
     * @param tx the Transaction to lift on.
     * @return the value stored in the ref.
     * @throws NullPointerException if tx is null.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     *                  if something failed while using the transaction. The transaction is guaranteed to have been aborted.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *                  if the Stm needs to control the flow in a different way than normal returns of exceptions. The transaction
     *                  is guaranteed to have been aborted.
     */
    boolean get(Transaction tx);

    boolean getAndLock(Transaction tx, LockMode lockMode);

    /**
    * Sets the new value using the Transaction on the ThreadLocalTransaction.
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
    * Sets the new value using the Transaction on the ThreadLocalTransaction and applies the lock.
    *
    * @param value the new value.
    * @param lockMode
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
    * @param tx    the transaction used to do the set.
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
    * @param tx    the transaction used to do the set.
    * @param value the new value
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
     * Sets the value and returns the previous value using the Transaction on the ThreadLocalTransaction.
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

    boolean getAndSetAndLock(boolean value, LockMode lockMode);

    /**
    * Sets the value using the provided transaction.
    *
    * @param value the new value.
    * @param tx    the transaction used to do the getAndSet.
    * @return the old value.
    * @throws NullPointerException if tx is null.
    * @throws org.multiverse.api.exceptions.TransactionExecutionException
    *                  if something failed while using the transaction. The transaction is guaranteed to have been aborted.
    * @throws org.multiverse.api.exceptions.ControlFlowError
    *                  if the Stm needs to control the flow in a different way than normal returns of exceptions. The transaction
    *                  is guaranteed to have been aborted.
    */
    boolean getAndSet(Transaction tx, boolean value);

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
     * <p/>
     * It is the best method to call if you just want to get the current value stored.
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
     * Applies the function on the re in a commuting manner. So if there are no dependencies, the function
     * will commute. If somehow there already is a dependency or a dependency is formed on the result of
     * the commuting function, the function will not commute and will be exactly the same as an alter.
     * <p/>
     * This is different than the behavior in Clojure where the commute will be re-applied at the end
     * of the transaction, even though some dependency is introduced, which can lead to inconsistencies.
     * <p/>
     * This call uses the Transaction stored in the ThreadLocalTransaction.
     * <p/>
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
     * <p/>
     * This is different than the behavior in Clojure where the commute will be re-applied at the end
     * of the transaction, even though some dependency is introduced, which can lead to inconsistencies.
     * <p/>
     * This call lifts on the Transaction stored in the ThreadLocalTransaction.
     * <p/>
     *
     * @param tx       the transaction used for this operation.
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
     * Atomically applies the function to alterAndGet the value stored in this ref. This method doesn't care about
     * any running transactions.
     *
     * @param function the Function responsible to alterAndGet the function.
     * @return the new value.
     * @throws NullPointerException if function is null.
     */
    boolean atomicAlterAndGet(BooleanFunction function);

    /**
     * Alters the value stored in this Ref using the alterAndGet function. It lifts on the Transaction stored in the
     * ThreadLocalTransaction.
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
     * Alters the value stored in this Ref using the alterAndGet function and lifting on the provided transaction.
     *
     * @param function the function that alters the value stored in this Ref.
     * @param tx       the Transaction used by this operation.
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
     * Atomically applies the function to alterAndGet the value stored in this ref. This method doesn't care about
     * any running transactions.
     *
     * @param function the Function responsible to alterAndGet the function.
     * @return the old value.
     * @throws NullPointerException if function is null.
     * @throws org.multiverse.api.exceptions.TransactionExecutionException
     */
    boolean atomicGetAndAlter(BooleanFunction function);

    /**
     * Alters the value stored in this Ref using the alterAndGet function. This call lifts on the Transaction stored
     * in the ThreadLocalTransaction.
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
     * Alters the value stored in this Ref using the alterAndGet function using the provided transaction.
     *
     * @param function the function that alters the value stored in this Ref.
     * @param tx       the Transaction used by this operation.
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
     * This call lifts on the Transaction in the ThreadLocalTransaction.
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
     * @param tx the transaction this method lifts on
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
     * Awaits until the predicate holds.  If the value already evaluates to true, the call continues
     * else a retry is done. If the predicate throws an exception, the transaction is aborted and the
     * throwable is propagated.
     *
     * @param tx the transaction used.
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
