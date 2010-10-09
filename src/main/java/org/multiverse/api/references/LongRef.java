package org.multiverse.api.references;

import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionalObject;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.api.predicates.LongPredicate;

/**
 * A Transactional Reference comparable to the <a href="http://clojure.org/refs">Clojure Ref</a>.
 *
 * @author Peter Veentjer.
 */
public interface LongRef extends TransactionalObject {

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
    long getAndSet(long value);

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
    long set(long value);

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
    long set(Transaction tx, long value);

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
    long get();

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
    long get(Transaction tx);

    /**
     * Atomically gets the value. The value could be stale as soon as it is returned. This
     * method doesn't care about any running transactions.
     *
     * @return the current value.
     */
    long atomicGet();

    /**
     * Atomically gets the value without providing any ordering guarantees. This method is extremely
     * cheap and will never fail. It is the best method to call if you just want to get the current
     * value stored.
     */
    long atomicWeakGet();

    /**
     * Atomically sets the value and returns the new value. This method doesn't care about any
     * running transactions.
     *
     * @param newValue the new value.
     * @return the new value.
     */
    long atomicSet(long newValue);

    /**
     * Atomically sets the value and returns the previous value. This method doesn't care about
     * any running transactions.
     *
     * @param newValue the new value.
     * @return the old value.
     */
    long atomicGetAndSet(long newValue);

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
    long getAndSet(Transaction tx, long value);

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
    void commute(LongFunction function);

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
    void commute(Transaction tx, LongFunction function);

    /**
     * Atomically applies the function to alterAndGet the value stored in this ref. This method doesn't care about
     * any running transactions.
     *
     * @param function the Function responsible to alterAndGet the function.
     * @return the new value.
     * @throws NullPointerException if function is null.
     */
    long atomicAlterAndGet(LongFunction function);

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
    long alterAndGet(LongFunction function);

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
    long alterAndGet(Transaction tx, LongFunction function);

    /**
     * Atomically applies the function to alterAndGet the value stored in this ref. This method doesn't care about
     * any running transactions.
     *
     * @param function the Function responsible to alterAndGet the function.
     * @return the old value.
     * @throws NullPointerException if function is null.
     */
    long atomicGetAndAlter(LongFunction function);

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
    long getAndAlter(LongFunction function);

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
    long getAndAlter(Transaction tx, LongFunction function);

    /**
     * Executes a compare and set atomically. This method doesn't care about any running transactions.
     *
     * @param expectedValue the expected value.
     * @param newValue      the new value.
     * @return true if the compareAndSwap was a success, false otherwise.
     */
    boolean atomicCompareAndSet(long expectedValue, long newValue);

    /**
     * Adds a deferred validator. A deferred validator is executed once the transaction commits, so it
     * allows the value stored in the reference to be inconsistent during the execution of the transaction. If the same
     * validator is added multiple times, it will be called multiple times.
     * <p/>
     * This call lifts on the transaction stored in the ThreadLocalTransaction.
     *
     * @param validator the LongPredicate to add.
     * @throws NullPointerException if validator or tx is null. If validator is null and transaction is not, the
     *                              transaction if aborted.
     */
    void addDeferredValidator(LongPredicate validator);

    /**
     * Adds a deferred validator. A deferred validator is executed once the transaction commits, so it
     * allows the value stored in the reference to be inconsistent during the execution of the transaction. If the same
     * validator is added multiple times, it will be called multiple times.
     * <p/>
     * This call lifts on the provided transaction.
     *
     * @param tx the Transaction this call lifts on
     *           param validator the LongPredicate to add.
     * @throws NullPointerException if validator or tx is null. If validator is null and transaction is not, the
     *                              transaction if aborted.
     */
    void addDeferredValidator(Transaction tx, LongPredicate validator);

    /**
     * Atomically adds a deferred validator. A deferred validator is executed once the transaction commits, so it
     * allows the value stored in the reference to be inconsistent during the execution of the transaction. If the same
     * validator is added multiple times, it will be called multiple times.
     *
     * @param validator the LongPredicate to add.
     * @throws NullPointerException if validator is null.
     * @throws TransactionalExecutionException
     *
     */
    void atomicAddDeferredValidator(LongPredicate validator);

    /**
     * Atomically increments the value and returns the old value. This method doesn't care about
     * any running transactions.
     *
     * @param amount the amount to increase with.
     * @return the old value.
     */
    long atomicGetAndIncrement(long amount);

    /**
     * Increments the value and returns the old value. This call lifts on Transaction in the ThreadLocalTransaction.
     *
     * @param amount the amount to increment with.
     * @return the old value.
     * @throws TransactionalExecutionException
     *
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    long getAndIncrement(long amount);

    /**
     * Increments the value and returns the old value. This call lifts on the provided Transaction.
     *
     * @param tx     the transaction used for this operation.
     * @param amount the amount to increment with.
     * @return the old value.
     * @throws NullPointerException if tx is null.
     * @throws TransactionalExecutionException
     *
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    long getAndIncrement(Transaction tx, long amount);

    /**
     * Atomically increments the value and returns the old value. This method doesn't care about any
     * running transactions.
     *
     * @param amount the amount to increment with.
     * @return the new value.
     */
    long atomicIncrementAndGet(long amount);

    /**
     * Increments and gets the new value. This call lifts on the Transaction stored in the ThreadLocalTransaction.
     *
     * @param amount the amount to increment with.
     * @return the new value.
     * @throws TransactionalExecutionException
     *
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    long incrementAndGet(long amount);

    /**
     * Increments and gets the new value. This call lifts on the Transaction stored in the ThreadLocalTransaction.
     *
     * @param tx     the Transaction used for this operation.
     * @param amount the amount to increment with.
     * @return the new value.
     * @throws NullPointerException if tx is null.
     * @throws TransactionalExecutionException
     *
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    long incrementAndGet(Transaction tx, long amount);


    void await(long value);

    void await(Transaction tx, long value);

    //todo: atomicAwait.
}
