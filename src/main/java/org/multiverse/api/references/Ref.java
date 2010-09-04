package org.multiverse.api.references;

import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionalObject;
import org.multiverse.api.functions.Function;

/**
 * A Transactional Reference.
 *
 * @param <E>
 */
public interface Ref<E> extends TransactionalObject {

    /**
     * Atomically applies the function to alter the value stored in this ref. This method doesn't care about
     * any running transactions.
     *
     * @param function the Function responsible to alter the function.
     * @return the new value.
     * @throws NullPointerException if function is null.
     */
    E atomicAlter(Function<E> function);

    /**
     * Alters the value stored in this Ref using the alter function. If a transaction is available it will
     * lift on that transaction, else it will be run under its own transaction.
     *
     * @param function the function that alters the value stored in this Ref.
     * @return the new value.
     * @throws NullPointerException if function is null.
     */
    E alter(Function<E> function);

    /**
     * Alters the value stored in this Ref using the alter function.
     *
     * @param function the function that alters the value stored in this Ref.
     * @param tx       the Transaction used by this operation.
     * @return the new value.
     * @throws NullPointerException if function or transaction is null.
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *                              if the transaction is not in the
     *                              correct state.
     */
    E alter(Transaction tx, Function<E> function);

    /**
     * Atomically. This method doesn't care about any running transactions.
     *
     * @param oldValue the old value.
     * @param newValue the new value.
     * @return true if the compareAndSwap was a success, false otherwise.
     */
    boolean atomicCompareAndSet(E oldValue, E newValue);

    /**
     * Atomically sets the value and returns the previous value. This method doesn't care about
     * any running transactions.
     *
     * @param value the new value.
     * @return the old value.
     */
    E atomicSet(E value);

    /**
     * Sets the value and returns the previous value. If a transaction is running, it will lift on that
     * transaction, else it will be executed atomically (so executed under its own transaction).
     *
     * @param value the new value.
     * @return the old value.
     */
    E set(E value);

    /**
     * Sets the value using the provided transaction.
     *
     * @param value the new value.
     * @param tx    the transaction used to do the set.
     * @return the old value.
     * @throws NullPointerException if tx is null.
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *                              if the transaction is not
     *                              in the correct state for this operation.
     */
    E set(Transaction tx, E value);

    /**
     * Atomically gets the value. The value could be stale as soon as it is returned. This
     * method doesn't care about any running transactions.
     *
     * @return the current value.
     */
    E atomicGet();

    /**
     * Gets the value. If a Transaction currently is running, this call will lift on that transaction. If no
     * Transaction is running, it will be run under its own transaction (so executed atomically).
     *
     * @return the current value.
     * @see #atomicGet()
     */
    E get();

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
    E get(Transaction tx);
}
