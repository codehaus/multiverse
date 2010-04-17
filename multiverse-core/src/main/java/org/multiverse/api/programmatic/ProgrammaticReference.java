package org.multiverse.api.programmatic;

import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;

/**
 * The ProgrammaticReference is a managed reference specially made for projects that
 * don't want to rely on instrumentation, but do want to have managed references.
 * <p/>
 * It provides a lot of methods and I don't think that exposing this method is
 * very wise. I would expect that some nice language sugar is added on top.
 * <p/>
 * If you also want to coordinate transactions without relying on instrumentation,
 * you need to have a look at the {@link org.multiverse.templates.TransactionTemplate}
 * <p/>
 * This ProgrammaticReference exposes in a lot of cases 3 types of methods:
 * <ol>
 * <li>the original one: this method behaves nicely. If a method is available
 * on the tranlocal it will lift on that. If there isn't, the method will be
 * executed in its own transaction. </li>
 * <li>one with a transaction as parameter: this method you want to use of you
 * dont want to rely on the ThreadLocalTransaction, but want to pass transactions
 * manually. These methods need to be used with an already running transaction</li>
 * <li>one with atomic as postfix: these methods are very very fast. They don't
 * look at an existing transaction at the ThreadLocalTransaction and will always
 * succeed. The performance is comparable to that of a normal CAS. So use it
 * very wisely </li>
 * </ol>
 * With the ProgrammaticReference it also is possible to have more than one
 * stm in the same classloader; as long as transactionalobjects from different
 * stms move into eachers space. If they do, you could have serious isolation
 * problems or other failures at hand. If in the future this sharing problem
 * will cause complaints, it is quite easy to add an STM field to each transactional
 * objects.
 *
 * @author Peter Veentjer
 */
public interface ProgrammaticReference<E> {

    // =================== get ============================

    /**
     * Gets the value. If a Transaction already is running, it will lift on that transaction,
     * if not the {@link #atomicGet} is used. In that cases it will be very very cheap (roughly
     * 2/3 of the performance of {@link #atomicGet()}. The reason why this call is more expensive
     * than the {@link #atomicGet()} is that a getThreadlocalTransaction needs to be called and
     * a check on the Transaction if that is running.
     *
     * @return the current value stored in this reference.
     * @throws IllegalThreadStateException if the current transaction isn't in the right state
     *                                     for this operation.
     * @throws org.multiverse.api.exceptions.StmControlFlowError
     *
     */
    E get();

    /**
     * Gets the value using the specified transaction.
     *
     * @param tx the Transaction used for reading the value.
     * @return the value currently stored, could be null.
     * @throws NullPointerException if tx is null
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *                              if the transaction is not
     *                              in the correct state for this operation.
     * @throws org.multiverse.api.exceptions.StmControlFlowError
     *
     */
    E get(Transaction tx);

    /**
     * Gets the value without looking at an existing transaction (it will run its 'own').
     * <p/>
     * This is a very cheap call since only one volatile read is needed an no additional
     * objects are created. On my machine I'm able to do 150.000.000 atomic gets per second
     * on a single thread to give some indication.
     * <p/>
     * The only expensive thing that needs to be done is a single volatile read. So it is in the same
     * league as a {@link java.util.concurrent.atomic.AtomicReference#get()}. To be more specific;
     * it would have the same performance as {@link java.util.concurrent.atomic.AtomicReferenceFieldUpdater#get(Object)}
     * since that is used under water.
     *
     * @return the current value.
     * @throws org.multiverse.api.exceptions.ReadConflict
     *          if something fails while loading the reference.
     */
    E atomicGet();

    // ========================= set =========================

    /**
     * Sets the new value on this reference. If an active transaction is available in the
     * ThreadLocalTransaction that will be used. If not, the {@link #atomicSet(Object)} is used
     * (which is very fast).
     *
     * @param newValue the new value to be stored in this reference. The newValue is allowed to
     *                 be null.
     * @return the value that is replaced, can be null.
     * @throws org.multiverse.api.exceptions.WriteConflict
     *                                     if something failed while committing. If the commit fails, nothing bad will happen.
     * @throws org.multiverse.api.exceptions.ReadConflict
     *                                     if something fails while loading the reference.
     * @throws IllegalThreadStateException if the transaction was not in the correct state for
     *                                     this operations. If the transaction in the TransactionThreadLocal is dead (so aborted or
     *                                     committed), a new transaction will be used.
     */
    E set(E newValue);

    /**
     * Sets the new value on this reference using the provided transaction.
     * <p/>
     * Use this call if you explicitly want to pass a transaction, instead of {@link #set(Object)}.
     *
     * @param tx       the transaction to use.
     * @param newValue the new value, and is allowed to be null.
     * @return the previous value stored in this reference.
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *                              if the transaction isn't in the correct state for this operation.
     * @throws org.multiverse.api.exceptions.ReadConflict
     *                              if something fails while loading the reference.
     * @throws NullPointerException if tx is null
     */
    E set(Transaction tx, E newValue);

    /**
     * Sets the new value on this ProgrammaticReference using its own transaction (so it doesn't
     * look at an existing transaction). This call is very fast (11M transactions/second
     * on my machine with a single thread.  It could be that this called is delayed because it
     * is retried. If you want to fail immediately, check the {@link #atomicCompareAndSet(Object, Object)}
     *
     * @param newValue the new value.
     * @return the old value.
     * @throws org.multiverse.api.exceptions.WriteConflict
     *          if something failed while committing. If the commit fails, nothing bad will happen.
     * @throws org.multiverse.api.exceptions.ReadConflict
     *          if something fails while loading the reference.
     */
    E atomicSet(E newValue);

    // ======================= compareAndSet ========================


    /**
     * @param newValue
     * @param update
     * @return
     */
    boolean atomicCompareAndSet(E newValue, E update);

    // ======================= isNull ========================

    /**
     * Checks if the value stored in this reference is null.
     * <p/>
     * If a Transaction currently is active, it will lift on that transaction. If not, the
     * atomicIsNull is used, so very very cheap. See the {@link #get()} for more info since
     * that method is used to retrieve the current value.
     *
     * @return true if the reference currently is null.
     * @throws org.multiverse.api.exceptions.ReadConflict
     *          if something fails while loading the reference.
     */
    boolean isNull();

    /**
     * Checks if the value stored in this reference is null using the provided transaction.
     *
     * @param tx the transaction used
     * @return true if the value is null, false otherwise.
     * @throws NullPointerException if tx is null.
     * @throws org.multiverse.api.exceptions.DeadTransactionException
     *                              is tx isn't active
     * @throws org.multiverse.api.exceptions.ReadConflict
     *                              if something fails while loading the reference.
     */
    boolean isNull(Transaction tx);

    /**
     * Checks if the value stored in this reference is null. This method doesn't lift on a
     * transaction, so it is very very cheap. See the {@link #atomicGet()} for more information.
     *
     * @return true if the reference is null, false otherwise.
     * @throws org.multiverse.api.exceptions.ReadConflict
     *          if something fails while loading the reference.
     */
    boolean atomicIsNull();

    // =============== getOrAwait ================================

    E getOrAwait();

    E getOrAwait(TransactionFactory txFactory);

    /**
     * @param tx
     * @return
     * @throws NullPointerException if tx is null
     */
    E getOrAwait(Transaction tx);


    // ======================= misc ===================

    String toString(Transaction tx);
}
