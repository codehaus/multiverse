package org.multiverse.api.programmatic;

import org.multiverse.api.Transaction;

/**
 * Using the ProgrammaticRefFactory it is possible to get access to the
 * 'manual' instrumented reference: ProgrammaticRef. This reference
 * can safely be used without relying on instrumentation. It also provides
 * access to methods not available in the normal instrumented classes.
 * <p/>
 * This functionality specially is added to integrate Multiverse in environments
 * where you don't want to rely on instrumentation but just want to have
 * a 'managed reference (for example if you want to integrate multiverse in a
 * different language than Java).
 * <p/>
 * Methods are thread safe.
 *
 * @author Peter Veentjer
 */
public interface ProgrammaticRefFactory {

    /**
     * Creates a new ProgrammaticLongRef.
     * <p/>
     * If a Transaction is running in the ThreadLocalTransaction that will be used. If none
     * is running this call is atomic.
     * <p/>
     * If the ProgrammaticLongRef leaves the transaction it was created in before
     * that transaction commits, you will get the
     * {@link org.multiverse.api.exceptions.UncommittedReadConflict}.
     *
     * @param value the initial value of the ProgrammaticLongRef.
     * @return the created ProgrammaticLongRef.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    ProgrammaticLongRef createLongRef(long value);

    /**
     * Creates a new ProgrammaticLongRef that is created in the context of the
     * provided transaction.
     *
     * @param tx    the transaction to use for creating this ProgrammaticLongRef.
     * @param value the initial value of the ProgrammaticLongRef.
     * @return the created ProgrammaticLongRef
     * @throws NullPointerException if tx is null.
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *                              if the transaction is
     *                              not in the correct state for this operation.
     */
    ProgrammaticLongRef createLongRef(Transaction tx, long value);

    /**
     * Atomically creates a new ProgrammaticLongRef. So this call doesn't look at the
     * Transaction stores in the ThreadLocalTransaction.
     * <p/>
     * This is the cheapest call for making a ProgrammaticLongRef, so in most cases this is
     * the one you want to use.
     *
     * @param value the value stored in the ProgrammaticLongRef.
     * @return the created ProgrammaticLongRef
     */
    ProgrammaticLongRef atomicCreateLongRef(long value);

    /**
     * Creates a new ProgrammaticRef with the provided value. If a
     * transaction already is running in the ThreadLocalTransaction, it will lift on that
     * transaction. If no transaction is running, then this method will be run atomic.
     * <p/>
     * If the ProgrammaticRef leaves the transaction it was created in before
     * that transaction commits, you will get the
     * {@link org.multiverse.api.exceptions.UncommittedReadConflict}.
     *
     * @param value the value stored in the reference (is allowed to be null).
     * @param <E>   the type of the value stored in the reference
     * @return the created ProgrammaticRef.
     */
    <E> ProgrammaticRef<E> createRef(E value);

    /**
     * Creates a new ProgrammaticRef with the provided value and lifting
     * on the provided transaction. This method is the one you want to use if you
     * don't want to deal with the TransactionThreadLocal to exchange the Transaction.
     * <p/>
     * If the ProgrammaticRef leaves the transaction it was created in before
     * that transaction commits, you will get the
     * {@link org.multiverse.api.exceptions.UncommittedReadConflict}.
     *
     * @param tx    the Transaction used to createReference the ProgrammaticRef.
     * @param value the value stored in the reference (is allowed to be null).
     * @param <E>   the type of the value stored in the reference
     * @return the created ProgrammaticRef.
     * @throws NullPointerException if t is null.
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *                              if t is not active.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    <E> ProgrammaticRef<E> createRef(Transaction tx, E value);

    /**
     * Creates a new ProgrammaticRef atomically. So this call doesn't look at the
     * {@link org.multiverse.api.ThreadLocalTransaction}.
     * <p/>
     * This is the cheapest call for making a ProgrammaticRef, so in most cases
     * this is the one you want to use.
     *
     * @param value the value stores in the reference (is allowed to be null).
     * @param <E>   type of the value
     * @return the created ProgrammaticRef.
     */
    <E> ProgrammaticRef<E> atomicCreateRef(E value);

    /**
     * Creates a new ProgrammaticRef with a null value atomically. So this call
     * doesn't look at the {@link org.multiverse.api.ThreadLocalTransaction}.
     * This is the cheapest call for making a ProgrammaticRef, so in most cases
     * this is the one you want to use.
     *
     * @return the created ProgrammaticRef.
     */
    <E> ProgrammaticRef<E> atomicCreateRef();
}
