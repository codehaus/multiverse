package org.multiverse.api.references;

import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionalObject;
import org.multiverse.api.functions.BooleanFunction;

/**
 * A Ref optimized for a boolean.
 *
 * @author Peter Veentjer.
 */
public interface BooleanRef extends TransactionalObject {

    /**
     * Ensures that when this ref is read in a transaction, no other transaction is able to write to this
     * reference. Once it is ensured, it is guaranteed to commit (unless the transaction aborts otherwise).
     * <p/>
     * This call expects a running transaction.
     *
     * @throws IllegalStateException
     * @throws org.multiverse.api.exceptions.ReadConflict
     *
     */
    void ensure();

    /**
     * Ensures that when this ref is read in a transaction, no other transaction is able to write to this
     * reference. Once it is ensured, it is guaranteed to commit (unless the transaction aborts otherwise).
     * <p/>
     * This call expects a running transaction.
     *
     * @param tx the Transaction used for this operation.
     * @throws NullPointerException if tx is null.
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *                              if the transaction is
     *                              not in the correct state for this operation.
     */
    void ensure(Transaction tx);

    /**
     * Applies the function on the re in a commuting manner. So if there are no dependencies, the function
     * will commute. If somehow there already is a dependency or a dependency is formed on the result of
     * the commuting function, the function will not commute and will be exactly the same as an alter.
     * <p/>
     * This is different than the behavior in Clojure where the commute will be re-applied at the end
     * of the transaction, even though some dependency is introduced, which can lead to inconsistencies.
     * <p/>
     * This call lifts on an existing transaction if available, else it will be run under its own transaction.
     * <p/>
     *
     * @param function the function to apply to this reference.
     * @throws NullPointerException if function is null.
     */
    void commute(BooleanFunction function);

    /**
     * Applies the function on the re in a commuting manner. So if there are no dependencies, the function
     * will commute. If somehow there already is a dependency or a dependency is formed on the result of
     * the commuting function, the function will not commute and will be exactly the same as an alter.
     * <p/>
     * This is different than the behavior in Clojure where the commute will be re-applied at the end
     * of the transaction, even though some dependency is introduced, which can lead to inconsistencies.
     * <p/>
     * This call lifts on an existing transaction if available, else it will be run under its own transaction.
     * <p/>
     *
     * @param tx       the transaction used for this operation.
     * @param function the function to apply to this reference.
     * @throws NullPointerException  if function is null.
     * @throws IllegalStateException if the transaction is not in the correct state for this operation.
     */
    void commute(Transaction tx, BooleanFunction function);

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
     * Alters the value stored in this Ref using the alterAndGet function. If a transaction is available it will
     * lift on that transaction, else it will be run under its own transaction.
     *
     * @param function the function that alters the value stored in this Ref.
     * @return the new value.
     * @throws NullPointerException if function is null.
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *                              if tx is not in the correct state
     *                              for this operation.
     */
    boolean alterAndGet(BooleanFunction function);

    /**
     * Alters the value stored in this Ref using the alterAndGet function.
     *
     * @param function the function that alters the value stored in this Ref.
     * @param tx       the Transaction used by this operation.
     * @return the new value.
     * @throws NullPointerException if function or transaction is null.
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *                              if the transaction is not in the
     *                              correct state.
     */
    boolean alterAndGet(Transaction tx, BooleanFunction function);

    /**
     * Atomically applies the function to alterAndGet the value stored in this ref. This method doesn't care about
     * any running transactions.
     *
     * @param function the Function responsible to alterAndGet the function.
     * @return the old value.
     * @throws NullPointerException if function is null.
     */
    boolean atomicGetAndAlter(BooleanFunction function);

    /**
     * Alters the value stored in this Ref using the alterAndGet function. If a transaction is available it will
     * lift on that transaction, else it will be run under its own transaction.
     *
     * @param function the function that alters the value stored in this Ref.
     * @return the old value.
     * @throws NullPointerException if function is null.
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *                              if tx is not in the correct state
     *                              for this operation.
     */
    boolean getAndAlter(BooleanFunction function);

    /**
     * Alters the value stored in this Ref using the alterAndGet function.
     *
     * @param function the function that alters the value stored in this Ref.
     * @param tx       the Transaction used by this operation.
     * @return the old value
     * @throws NullPointerException if function or transaction is null.
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *                              if the transaction is not in the
     *                              correct state.
     */
    boolean getAndAlter(Transaction tx, BooleanFunction function);

    /**
     * Atomically. This method doesn't care about any running transactions.
     *
     * @param oldValue the old value.
     * @param newValue the new value.
     * @return true if the compareAndSwap was a success, false otherwise.
     */
    boolean atomicCompareAndSet(boolean oldValue, boolean newValue);

    /**
     * Atomically sets the value and returns the previous value. This method doesn't care about
     * any running transactions.
     *
     * @param value the new value.
     * @return the old value.
     */
    boolean atomicGetAndSet(boolean value);

    /**
     * Atomically sets the value and returns the new value. This method doesn't care about any
     * running transactions.
     *
     * @param value the new value.
     * @return the new value.
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *          if tx is not in the correct state
     *          for this operation.
     */
    boolean atomicSet(boolean value);

    /**
     * Sets the value and returns the previous value. If a transaction is running, it will lift on that
     * transaction, else it will be executed atomically (so executed under its own transaction).
     *
     * @param value the new value.
     * @return the old value.
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *          if tx is not in the correct state
     *          for this operation.
     */
    boolean getAndSet(boolean value);

    /**
     * Sets the new value. If a transaction is running, it will lift on that transaction, else it will
     * be executed atomically (so executed under its own transaction).
     *
     * @param value the new value.
     * @return the new value.
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *          if tx is not in the correct state
     *          for this operation.
     */
    boolean set(boolean value);

    /**
     * Sets the value using the provided transaction.
     *
     * @param value the new value.
     * @param tx    the transaction used to do the getAndSet.
     * @return the old value.
     * @throws NullPointerException if tx is null.
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *                              if the transaction is not
     *                              in the correct state for this operation.
     */
    boolean getAndSet(Transaction tx, boolean value);

    /**
     * Sets the new value using the provided transaction.
     *
     * @param tx    the transaction used to do the set.
     * @param value the new value
     * @return the new value
     * @throws NullPointerException if tx is null.
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *                              if the transaction is not in the correct
     *                              state for this operation.
     */
    boolean set(Transaction tx, boolean value);

    /**
     * Atomically gets the value. The value could be stale as soon as it is returned. This
     * method doesn't care about any running transactions.
     *
     * @return the current value.
     */
    boolean atomicGet();

    /**
     * Gets the value. If a Transaction currently is running, this call will lift on that transaction. If no
     * Transaction is running, it will be run under its own transaction (so executed atomically).
     *
     * @return the current value.
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *          if tx is not in the correct state
     *          for this operation.
     * @see #atomicGet()
     */
    boolean get();

    /**
     * Gets the value using the provided transaction.
     *
     * @param tx the Transaction to lift on.
     * @return the value stored in the ref.
     * @throws NullPointerException if tx is null.
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *                              if the transaction is not in the
     *                              correct state for this operation.
     */
    boolean get(Transaction tx);

    /**
     * @param value
     */
    void await(boolean value);

    /**
     * @param value
     */
    void await(Transaction tx, boolean value);
}
