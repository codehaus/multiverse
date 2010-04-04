package org.multiverse.api.programmatic;

import org.multiverse.api.Transaction;

/**
 * Using the ProgrammaticReferenceFactory it is possible to get access to the
 * 'manual' instrumented reference: ProgrammaticReference. This reference
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
public interface ProgrammaticReferenceFactory {

    /**
     * Creates a new ProgrammaticLong.
     * <p/>
     * If a Transaction is running in the ThreadLocalTransaction that will be used. If none
     * is running this call is atomic.
     * <p/>
     * If the ProgrammaticLong leaves the transaction it was created in before
     * that transaction commits, you will get the
     * {@link org.multiverse.api.exceptions.UncommittedReadConflict}.
     *
     * @param value the initial value of the ProgrammaticLong.
     * @return the created ProgrammaticLong.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    ProgrammaticLong createLong(long value);

    /**
     * Creates a new ProgrammaticLong.
     *
     * @param tx    the transaction to use for creating this ProgrammaticLong.
     * @param value the initial value of the ProgrammaticLong.
     * @return the created ProgrammaticLong
     * @throws NullPointerException if tx is null.
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *                              if the transaction is
     *                              not in the correct state for this operation.
     */
    ProgrammaticLong createLong(Transaction tx, long value);

    /**
     * Atomically creates a new ProgrammaticLong. So this call doesn't look at the
     * Transaction stores in the ThreadLocalTransaction.
     *
     * @param value the value stored in the ProgrammaticLong.
     * @return the created ProgrammaticLong
     */
    ProgrammaticLong atomicCreateLong(long value);

    /**
     * Creates a new ProgrammaticReference with the provided value. If a
     * transaction already is running in the ThreadLocalTransaction, it will lift on that
     * transaction. If no transaction is running, then this method will be run atomic.
     * <p/>
     * If the ProgrammaticReference leaves the transaction it was created in before
     * that transaction commits, you will get the
     * {@link org.multiverse.api.exceptions.UncommittedReadConflict}.
     *
     * @param value the value stored in the reference (is allowed to be null).
     * @param <E>   the type of the value stored in the reference
     * @return the created ProgrammaticReference.
     */
    <E> ProgrammaticReference<E> createReference(E value);

    /**
     * Creates a new ProgrammaticReference with the provided value and lifting
     * on the provided transaction. This method is the one you want to use if you
     * don't want to deal with the TransactionThreadLocal to exchange the Transaction.
     * <p/>
     * If the ProgrammaticReference leaves the transaction it was created in before
     * that transaction commits, you will get the
     * {@link org.multiverse.api.exceptions.UncommittedReadConflict}.
     *
     * @param tx    the Transaction used to createReference the ProgrammaticReference.
     * @param value the value stored in the reference (is allowed to be null).
     * @param <E>   the type of the value stored in the reference
     * @return the created ProgrammaticReference.
     * @throws NullPointerException if t is null.
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *                              if t is not active.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    <E> ProgrammaticReference<E> createReference(Transaction tx, E value);

    /**
     * Creates a new ProgrammaticReference atomically. So this call doesn't look at the
     * {@link org.multiverse.api.ThreadLocalTransaction}.
     *
     * @param value the value stores in the reference (is allowed to be null).
     * @param <E>   type of the value
     * @return the created ProgrammaticReference.
     */
    <E> ProgrammaticReference<E> atomicCreateReference(E value);
}
