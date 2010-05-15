package org.multiverse.api.programmatic;

import org.multiverse.api.Transaction;

/**
 * A transactional long that can be used to integrate Multiverse in environments where instrumentation
 * is not desired or to provide access to features not yet integrated in the instrumentation (for
 * example the commutingInc support).
 * <p/>
 * It can be compared to the {@link java.util.concurrent.atomic.AtomicLong} except that this
 * ProgrammaticLong also is able to participate in full blown transactions.
 * <p/>
 * A big reason why this ProgrammaticLong is added, is that it can be used as a size field in
 * Transactional collections. Normally collections conflict on the size field even if other parts of
 * the operations can be executed in parallel. Using the commutingInc methods it is possible to
 * createReference commuting operations that prevent the unwanted conflict.
 * <p/>
 * In Multiverse 0.6 commuting operations will be made available on a general level, but for the
 * time being this gives the biggest bang for the buck.
 *
 * @author Peter Veentjer
 */
public interface ProgrammaticLong {

    // ================= set =======================

    /**
     * Gets the current value stores in this ProgrammaticLong.
     * <p/>
     * If an active transaction is available in the ThreadLocalTransaction, that will be used.
     * If none is available, the get will be atomic (so be executed using its private transaction).
     * <p/>
     * It is important to realize that getting a value could jeopardize the commuting behavior of
     * this ProgrammaticLong. If you have executed commutingInc and then call this method, the operation
     * will not commute anymore.
     *
     * @return the current value.
     * @throws org.multiverse.api.exceptions.ReadConflict
     *
     * @throws IllegalThreadStateException if the transaction is not active.
     */
    long get();

    /**
     * Gets the current value stores in this ProgrammaticLong.
     * <p/>
     * This call doesn't look at the ThreadLocalTransaction, so you have complete control on the
     * transaction used.
     * <p/>
     * It is important to realize that getting a value could jeopardize the commuting behavior of
     * this ProgrammaticLong. If you have executed commutingInc and then call this method, the operation
     * will not commute anymore.
     *
     * @param tx the transaction to use.
     * @return the current stored value.
     * @throws IllegalThreadStateException if the transaction is not active.
     * @throws NullPointerException        if tx is null.
     * @throws org.multiverse.api.exceptions.ReadConflict
     *
     */
    long get(Transaction tx);

    /**
     * Gets the last committed value atomically without looking at a Transaction stored in the
     * ThreadLocalTransaction.
     * <p/>
     * If this call is done inside a transaction, it is possible that this call returns a value
     * that is not correct in the eyes of the transaction. So careless use can lead to isolation
     * problems, but correct use could allow for better scaling datastructures.
     * <p/>
     * It is very likely that this call is very cheap since it doesn't need a full blown
     * transaction.
     *
     * @return the last committed value.
     */
    long atomicGet();

    // ================= set =======================

    /**
     * Sets the new value.
     * <p/>
     * If a transaction currently is available in the ThreadLocalTransaction, it will be used.
     * Otherwise the set will be atomic.
     *
     * @param newValue the new value to store in this ProgrammaticLong.
     * @return the old value.
     */
    long set(long newValue);

    /**
     * This call doesn't look at the ThreadLocalTransaction, so you have complete control on the
     * transaction used.
     *
     * @param tx       the Transaction to use.
     * @param newValue the new value
     * @return the old value
     * @throws IllegalThreadStateException
     * @throws NullPointerException        if tx is null
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     */
    long set(Transaction tx, long newValue);

    /**
     * Sets the new value and completely ignores the Transaction stored in the ThreadLocalTransaction.
     * If the value is not changed, it is free for the implementation to ignore this call.
     * <p/>
     * It is very likely that this call is very cheap since it doesn't need a full blown
     * transaction, but it could be that multiple retries are attempted and the transaction is
     * backed (delayed) on failure.
     *
     * @param newValue the new value
     *                 todo: exceptions
     * @return the old value.
     */
    long atomicSet(long newValue);

    // ================= increment =======================

    /**
     * Executes  a compare and set operation atomically, so it doesn't look at a running
     * transaction stored in the ThreadLocalTransaction.
     * <p/>
     * Provides the same behavior as the
     * {@link java.util.concurrent.atomic.AtomicLong#compareAndSet(long, long)}.
     *
     * @param expected the expected value.
     * @param update   the new value
     * @return true of the compare and swap was a success, false otherwise.
     */
    boolean atomicCompareAndSet(long expected, long update);

    // ================= increment =======================

    /**
     * Increments the counter with the specified amount.
     * <p/>
     * If a transaction currently is running in the ThreadLocalTransaction, that transaction
     * will be used, otherwise this call will be atomic.
     *
     * @param amount the amount to increase the value with.
     */
    void inc(long amount);

    /**
     * Increments the value stored in this ProgrammaticLong using the provided transaction.
     * <p/>
     * This call doesn't look at the ThreadLocalTransaction, so you have complete control on the
     * transaction used.
     *
     * @param tx     the Transaction to use.
     * @param amount the amount the increase this ProgrammaticLong with.
     * @throws NullPointerException if tx is null.
     */
    void inc(Transaction tx, long amount);

    /**
     * Atomically increments the value stores in this ProgrammaticLong. This call doesn't look
     * at the ThreadLocalTransaction for a running transaction, but will execute inside its own
     * (so is atomic).
     * <p/>
     * It is very likely that this call is very cheap since it doesn't need a full blown
     * transaction.
     *
     * @param amount the amount to increase the value with.
     */
    void atomicInc(long amount);

    /**
     * Increments the value stored in this ProgrammaticReference.
     * <p/>
     * If a transaction is available in the ThreadLocalTransaction, that will be used. Otherwise
     * this call will be executed atomic.
     *
     * @param amount the amount to increase with.
     */
    void commutingInc(long amount);

    /**
     * Increments the value stored in this ProgrammaticLong. The cool thing is that the increment
     * is a commuting operation (so transactions that otherwise would conflict, can still both commit
     * if the conflicting operations can commute).
     *
     * @param tx     the transaction to use.
     * @param amount the amount to increase the value with
     * @throws NullPointerException        if tx is null.
     * @throws IllegalThreadStateException if not in the correct state for this operation.
     */
    void commutingInc(Transaction tx, long amount);

    void changeMod();

    void changeMod(Transaction tx);

    void atomicChangeMod();
}
