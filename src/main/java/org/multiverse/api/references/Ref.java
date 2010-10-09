package org.multiverse.api.references;

import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionalObject;
import org.multiverse.api.functions.Function;
import org.multiverse.api.predicates.Predicate;

/**
 * A Transactional Reference comparable to the <a href="http://clojure.org/refs">Clojure Ref</a>.
 *
 * @author Peter Veentjer.
 * @param <E>
 */
public interface Ref<E> extends TransactionalObject {

    /**
     * Sets the value and returns the previous value using the Transaction on the ThreadLocalTransaction.
     *
     * @param value the new value.
     * @return the old value.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     * @throws TransactionalExecutionException
     *
     */
    E getAndSet(E value);

    /**
     * Sets the new value using the Transaction on the ThreadLocalTransaction.
     *
     * @param value the new value.
     * @return the new value.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     * @throws TransactionalExecutionException
     *
     */
    E set(E value);

    /**
     * Sets the new value using the provided transaction.
     *
     * @param tx    the transaction used to do the set.
     * @param value the new value
     * @return the old value
     * @throws NullPointerException if tx is null.
     * @throws TransactionalExecutionException
     *
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    E set(Transaction tx, E value);

    /**
     * Gets the value using the provided transaction.
     *
     * @return the current value.
     * @throws TransactionalExecutionException
     *
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     * @see #atomicGet()
     */
    E get();

    /**
     * Gets the value using the provided transaction.
     *
     * @param tx the Transaction to lift on.
     * @return the value stored in the ref.
     * @throws NullPointerException if tx is null.
     * @throws TransactionalExecutionException
     *
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    E get(Transaction tx);

    /**
     * Atomically gets the value. The value could be stale as soon as it is returned. This
     * method doesn't care about any running transactions.
     *
     * @return the current value.
     */
    E atomicGet();

    /**
     * Atomically gets the value without providing any ordering guarantees. This method is extremely
     * cheap and will never fail. It is the best method to call if you just want to get the current
     * value stored.
     */
    E atomicWeakGet();

    /**
     * Atomically sets the value and returns the new value. This method doesn't care about any
     * running transactions.
     *
     * @param newValue the new value.
     * @return the new value.
     */
    E atomicSet(E newValue);

    /**
     * Atomically sets the value and returns the previous value. This method doesn't care about
     * any running transactions.
     *
     * @param newValue the new value.
     * @return the old value.
     */
    E atomicGetAndSet(E newValue);

    /**
     * Sets the value using the provided transaction.
     *
     * @param value the new value.
     * @param tx    the transaction used to do the getAndSet.
     * @return the old value.
     * @throws NullPointerException if tx is null.
     * @throws TransactionalExecutionException
     *
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    E getAndSet(Transaction tx, E value);

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
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     * @throws TransactionalExecutionException
     *
     */
    void commute(Function<E> function);

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
     * @throws NullPointerException if function is null.
     * @throws TransactionalExecutionException
     *
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    void commute(Transaction tx, Function<E> function);

    /**
     * Atomically applies the function to alterAndGet the value stored in this ref. This method doesn't care about
     * any running transactions.
     *
     * @param function the Function responsible to alterAndGet the function.
     * @return the new value.
     * @throws NullPointerException if function is null.
     */
    E atomicAlterAndGet(Function<E> function);

    /**
     * Alters the value stored in this Ref using the alterAndGet function. It lifts on the Transaction stored in the
     * ThreadLocalTransaction.
     *
     * @param function the function that alters the value stored in this Ref.
     * @return the new value.
     * @throws NullPointerException if function is null.
     * @throws TransactionalExecutionException
     *
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    E alterAndGet(Function<E> function);

    /**
     * Alters the value stored in this Ref using the alterAndGet function and lifting on the provided transaction.
     *
     * @param function the function that alters the value stored in this Ref.
     * @param tx       the Transaction used by this operation.
     * @return the new value.
     * @throws NullPointerException if function or transaction is null.
     * @throws TransactionalExecutionException
     *
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    E alterAndGet(Transaction tx, Function<E> function);

    /**
     * Atomically applies the function to alterAndGet the value stored in this ref. This method doesn't care about
     * any running transactions.
     *
     * @param function the Function responsible to alterAndGet the function.
     * @return the old value.
     * @throws NullPointerException if function is null.
     */
    E atomicGetAndAlter(Function<E> function);

    /**
     * Alters the value stored in this Ref using the alterAndGet function. This call lifts on the Transaction stored
     * in the ThreadLocalTransaction.
     *
     * @param function the function that alters the value stored in this Ref.
     * @return the old value.
     * @throws NullPointerException if function is null.
     * @throws TransactionalExecutionException
     *
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    E getAndAlter(Function<E> function);

    /**
     * Alters the value stored in this Ref using the alterAndGet function using the provided transaction.
     *
     * @param function the function that alters the value stored in this Ref.
     * @param tx       the Transaction used by this operation.
     * @return the old value
     * @throws NullPointerException if function or transaction is null.
     * @throws TransactionalExecutionException
     *
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    E getAndAlter(Transaction tx, Function<E> function);

    /**
     * Executes a compare and set atomically. This method doesn't care about any running transactions.
     *
     * @param expectedValue the expected value.
     * @param newValue      the new value.
     * @return true if the compareAndSwap was a success, false otherwise.
     */
    boolean atomicCompareAndSet(E expectedValue, E newValue);

    /**
     * Adds a deferred validator. A deferred validator is executed once the transaction commits, so it
     * allows the value stored in the reference to be inconsistent during the execution of the transaction. If the same
     * validator is added multiple times, it will be called multiple times.
     * <p/>
     * This call lifts on the transaction stored in the ThreadLocalTransaction.
     *
     * @param validator the Predicate<E> to add.
     * @throws NullPointerException if validator or tx is null. If validator is null and transaction is not, the
     *                              transaction if aborted.
     */
    void addDeferredValidator(Predicate<E> validator);

    /**
     * Adds a deferred validator. A deferred validator is executed once the transaction commits, so it
     * allows the value stored in the reference to be inconsistent during the execution of the transaction. If the same
     * validator is added multiple times, it will be called multiple times.
     * <p/>
     * This call lifts on the provided transaction.
     *
     * @param tx the Transaction this call lifts on
     *           param validator the Predicate<E> to add.
     * @throws NullPointerException if validator or tx is null. If validator is null and transaction is not, the
     *                              transaction if aborted.
     */
    void addDeferredValidator(Transaction tx, Predicate<E> validator);

    /**
     * Atomically adds a deferred validator. A deferred validator is executed once the transaction commits, so it
     * allows the value stored in the reference to be inconsistent during the execution of the transaction. If the same
     * validator is added multiple times, it will be called multiple times.
     *
     * @param validator the Predicate<E> to add.
     * @throws NullPointerException if validator is null.
     * @throws TransactionalExecutionException
     *
     */
    void atomicAddDeferredValidator(Predicate<E> validator);

    /**
     * Checks if the current value is null. This call lifts on the Transaction stored in the ThreadLocalTransaction.
     *
     * @return true if null, false otherwise.
     * @throws TransactionalExecutionException
     *
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    boolean isNull();

    /**
     * Checks if the current value is null. This call lifts on the provided Transaction.
     *
     * @param tx the transaction used for this operation.
     * @return true if the value is null, false otherwise.
     * @throws NullPointerException if tx is null.
     * @throws TransactionalExecutionException
     *
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    boolean isNull(Transaction tx);

    /**
     * Atomically check if the current value is null. This method doesn't care about any running transactions.
     *
     * @return true if null, false otherwise.
     */
    boolean atomicIsNull();

    void await(E value);

    void await(Transaction tx, E value);

    //todo: atomicAwait.
}
