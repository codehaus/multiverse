package org.multiverse.api;

import org.multiverse.api.closures.*;
import org.multiverse.api.collections.*;
import org.multiverse.api.exceptions.ControlFlowError;
import org.multiverse.api.exceptions.IllegalTransactionStateException;
import org.multiverse.api.exceptions.TransactionRequiredException;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.api.references.*;

import java.util.Collection;

import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.getRequiredThreadLocalTransaction;

/**
 * A utility class with convenience methods to access the {@link org.multiverse.api.Stm} or
 * {@link Transaction}. These methods can be imported using the static import for a less
 * ugly syntax.
 *
 * @author Peter Veentjer.
 */
public class StmUtils {

    private final static RefFactory refFactory
        = getGlobalStmInstance().getDefaultRefFactory();
    private final static AtomicBlock defaultAtomicBlock
        = getGlobalStmInstance().getDefaultAtomicBlock();
    private final static TransactionalCollectionsFactory collectionsFactory
        = getGlobalStmInstance().getCollectionsFactory();

    /**
     * Creates a new committed TransactionalList that is based on linked nodes using the GlobalStmInstance.
     *
     * @param <E> the type of the elements in the list.
     * @return the created list.
     */
    public static <E> TransactionalList<E> newLinkedList(){
        return collectionsFactory.newLinkedList();
    }

     /**
     * Creates a new committed TransactionalList that is based on linked nodes and
     * is initialized with the given collection and using the GlobalStmInstance.
     *
     * @param c   the initial collection.
     * @param <E> the type of the elements in the list.
     * @return the created list.
     * @throws NullPointerException if c is null.
     */
    public static <E> TransactionalList<E> newLinkedList(Collection<? extends E> c){
        return collectionsFactory.newLinkedList(c);
    }

    public static <E> TransactionalDeque<E> newLinkedDeque(){
        return collectionsFactory.newLinkedDeque();
    }

    public static <E> TransactionalDeque<E> newLinkedDeque(Collection<? extends E> c){
        return collectionsFactory.newLinkedDeque(c);
    }

    public static <E> TransactionalBlockingDeque<E> newLinkedBlockingDeque(){
        return collectionsFactory.newLinkedBlockingDeque();
    }

    public static <E> TransactionalBlockingDeque<E> newLinkedBlockingDeque(int capacity){
        return collectionsFactory.newLinkedBlockingDeque(capacity);
    }

    public static <E> TransactionalBlockingDeque<E> newLinkedBlockingDeque(Collection<? extends E> c){
        return collectionsFactory.newLinkedBlockingDeque(c);
    }

    public static <E> TransactionalQueue<E> newLinkedQueue(){
        return collectionsFactory.newLinkedQueue();
    }

    public static <E> TransactionalQueue<E> newLinkedQueue(Collection<? extends E> c){
        return collectionsFactory.newLinkedQueue(c);
    }

    public static <E> TransactionalBlockingQueue<E> newLinkedBlockingQueue(){
        return collectionsFactory.newLinkedBlockingQueue();
    }

    public static <E> TransactionalBlockingQueue<E> newLinkedBlockingQueue(int capacity){
        return collectionsFactory.newLinkedBlockingQueue(capacity);
    }

    public static <E> TransactionalBlockingQueue<E> newLinkedBlockingQueue(Collection<? extends E> c){
        return collectionsFactory.newLinkedBlockingQueue(c);
    }

    /**
     * Executes the closure transactionally on the GlobalStmInstance using the default AtomicBlock. If a
     * Transaction already is active on the ThreadLocalTransaction, this transaction will lift on that
     * transaction (so the propagation level is Requires) and will not commit this transaction.
     *
     * This method doesn't throw a checked exception, but if the closure does, it is wrapped inside an
     * InvisibleCheckedException.
     *
     * @param closure The {AtomicClosure} to execute.
     * @return the result of the execution
     * @throws NullPointerException if closure is null.
     * @throws org.multiverse.api.exceptions.InvisibleCheckedException
     *                                  if the closure throws a checked exception.
     */
    public static <E> E execute(AtomicClosure<E> closure){
        return defaultAtomicBlock.execute(closure);
    }

   /**
    * Executes the closure transactionally on the GlobalStmInstance using the default AtomicBlock. If a
    * Transaction already is active on the ThreadLocalTransaction, this transaction will lift on that
    * transaction (so the propagation level is Requires) and will not commit this transaction.
    *
    * @param closure The {AtomicClosure} to execute.
    * @return the result of the execution
    * @throws NullPointerException if closure is null.
    * @throws Exception is the closure throws an Exception
    */
   public static <E> E executeChecked(AtomicClosure<E> closure) throws Exception{
       return defaultAtomicBlock.executeChecked(closure);
   }

    /**
     * Executes the closure transactionally on the GlobalStmInstance using the default AtomicBlock. If a
     * Transaction already is active on the ThreadLocalTransaction, this transaction will lift on that
     * transaction (so the propagation level is Requires) and will not commit this transaction.
     *
     * This method doesn't throw a checked exception, but if the closure does, it is wrapped inside an
     * InvisibleCheckedException.
     *
     * @param closure The {AtomicIntClosure} to execute.
     * @return the result of the execution
     * @throws NullPointerException if closure is null.
     * @throws org.multiverse.api.exceptions.InvisibleCheckedException
     *                                  if the closure throws a checked exception.
     */
    public static  int execute(AtomicIntClosure closure){
        return defaultAtomicBlock.execute(closure);
    }

   /**
    * Executes the closure transactionally on the GlobalStmInstance using the default AtomicBlock. If a
    * Transaction already is active on the ThreadLocalTransaction, this transaction will lift on that
    * transaction (so the propagation level is Requires) and will not commit this transaction.
    *
    * @param closure The {AtomicIntClosure} to execute.
    * @return the result of the execution
    * @throws NullPointerException if closure is null.
    * @throws Exception is the closure throws an Exception
    */
   public static  int executeChecked(AtomicIntClosure closure) throws Exception{
       return defaultAtomicBlock.executeChecked(closure);
   }

    /**
     * Executes the closure transactionally on the GlobalStmInstance using the default AtomicBlock. If a
     * Transaction already is active on the ThreadLocalTransaction, this transaction will lift on that
     * transaction (so the propagation level is Requires) and will not commit this transaction.
     *
     * This method doesn't throw a checked exception, but if the closure does, it is wrapped inside an
     * InvisibleCheckedException.
     *
     * @param closure The {AtomicLongClosure} to execute.
     * @return the result of the execution
     * @throws NullPointerException if closure is null.
     * @throws org.multiverse.api.exceptions.InvisibleCheckedException
     *                                  if the closure throws a checked exception.
     */
    public static  long execute(AtomicLongClosure closure){
        return defaultAtomicBlock.execute(closure);
    }

   /**
    * Executes the closure transactionally on the GlobalStmInstance using the default AtomicBlock. If a
    * Transaction already is active on the ThreadLocalTransaction, this transaction will lift on that
    * transaction (so the propagation level is Requires) and will not commit this transaction.
    *
    * @param closure The {AtomicLongClosure} to execute.
    * @return the result of the execution
    * @throws NullPointerException if closure is null.
    * @throws Exception is the closure throws an Exception
    */
   public static  long executeChecked(AtomicLongClosure closure) throws Exception{
       return defaultAtomicBlock.executeChecked(closure);
   }

    /**
     * Executes the closure transactionally on the GlobalStmInstance using the default AtomicBlock. If a
     * Transaction already is active on the ThreadLocalTransaction, this transaction will lift on that
     * transaction (so the propagation level is Requires) and will not commit this transaction.
     *
     * This method doesn't throw a checked exception, but if the closure does, it is wrapped inside an
     * InvisibleCheckedException.
     *
     * @param closure The {AtomicDoubleClosure} to execute.
     * @return the result of the execution
     * @throws NullPointerException if closure is null.
     * @throws org.multiverse.api.exceptions.InvisibleCheckedException
     *                                  if the closure throws a checked exception.
     */
    public static  double execute(AtomicDoubleClosure closure){
        return defaultAtomicBlock.execute(closure);
    }

   /**
    * Executes the closure transactionally on the GlobalStmInstance using the default AtomicBlock. If a
    * Transaction already is active on the ThreadLocalTransaction, this transaction will lift on that
    * transaction (so the propagation level is Requires) and will not commit this transaction.
    *
    * @param closure The {AtomicDoubleClosure} to execute.
    * @return the result of the execution
    * @throws NullPointerException if closure is null.
    * @throws Exception is the closure throws an Exception
    */
   public static  double executeChecked(AtomicDoubleClosure closure) throws Exception{
       return defaultAtomicBlock.executeChecked(closure);
   }

    /**
     * Executes the closure transactionally on the GlobalStmInstance using the default AtomicBlock. If a
     * Transaction already is active on the ThreadLocalTransaction, this transaction will lift on that
     * transaction (so the propagation level is Requires) and will not commit this transaction.
     *
     * This method doesn't throw a checked exception, but if the closure does, it is wrapped inside an
     * InvisibleCheckedException.
     *
     * @param closure The {AtomicBooleanClosure} to execute.
     * @return the result of the execution
     * @throws NullPointerException if closure is null.
     * @throws org.multiverse.api.exceptions.InvisibleCheckedException
     *                                  if the closure throws a checked exception.
     */
    public static  boolean execute(AtomicBooleanClosure closure){
        return defaultAtomicBlock.execute(closure);
    }

   /**
    * Executes the closure transactionally on the GlobalStmInstance using the default AtomicBlock. If a
    * Transaction already is active on the ThreadLocalTransaction, this transaction will lift on that
    * transaction (so the propagation level is Requires) and will not commit this transaction.
    *
    * @param closure The {AtomicBooleanClosure} to execute.
    * @return the result of the execution
    * @throws NullPointerException if closure is null.
    * @throws Exception is the closure throws an Exception
    */
   public static  boolean executeChecked(AtomicBooleanClosure closure) throws Exception{
       return defaultAtomicBlock.executeChecked(closure);
   }

    /**
     * Executes the closure transactionally on the GlobalStmInstance using the default AtomicBlock. If a
     * Transaction already is active on the ThreadLocalTransaction, this transaction will lift on that
     * transaction (so the propagation level is Requires) and will not commit this transaction.
     *
     * This method doesn't throw a checked exception, but if the closure does, it is wrapped inside an
     * InvisibleCheckedException.
     *
     * @param closure The {AtomicVoidClosure} to execute.
     * @throws NullPointerException if closure is null.
     * @throws org.multiverse.api.exceptions.InvisibleCheckedException
     *                                  if the closure throws a checked exception.
     */
    public static  void execute(AtomicVoidClosure closure){
        defaultAtomicBlock.execute(closure);
    }

   /**
    * Executes the closure transactionally on the GlobalStmInstance using the default AtomicBlock. If a
    * Transaction already is active on the ThreadLocalTransaction, this transaction will lift on that
    * transaction (so the propagation level is Requires) and will not commit this transaction.
    *
    * @param closure The {AtomicVoidClosure} to execute.
    * @throws NullPointerException if closure is null.
    * @throws Exception is the closure throws an Exception
    */
   public static  void executeChecked(AtomicVoidClosure closure) throws Exception{
       defaultAtomicBlock.executeChecked(closure);
   }

    /**
     * Creates a committed IntRef with the provided value using the GlobalStmInstance.
     *
     * @param value the initial value of the IntRef
     * @return the created IntRef.
     */
    public static IntRef newIntRef(int value) {
        return refFactory.newIntRef(value);
    }

    /**
     * Creates a committed IntRef with 0 as initial value using the GlobalStmInstance.
     *
     * @return the created IntRef.
     */
    public static IntRef newIntRef() {
        return refFactory.newIntRef(0);
    }

    /**
     * Creates a committed LongRef with 0 as initial value using the GlobalStmInstance.
     *
     * @return the created LongRef.
     */
    public static LongRef newLongRef() {
        return refFactory.newLongRef(0);
    }

    /**
     * Creates a committed LongRef with the provided value using the GlobalStmInstance.
     *
     * @param value the initial value of the LongRef.
     * @return the created LongRef.
     */
    public static LongRef newLongRef(long value) {
        return refFactory.newLongRef(value);
    }

    /**
     * Creates a committed DoubleRef with 0 as initial value using the GlobalStmInstance.
     *
     * @return the created DoubleRef.
     */
    public static DoubleRef newDoubleRef() {
        return refFactory.newDoubleRef(0);
    }

    /**
     * Creates a committed DoubleRef with the provided value using the GlobalStmInstance.
     *
     * @param value the initial value.
     * @return the created DoubleRef.
     */
    public static DoubleRef newDoubleRef(double value) {
        return refFactory.newDoubleRef(value);
    }

    /**
     * Creates a committed BooleanRef with false as initial value using the GlobalStmInstance.
     *
     * @return the created BooleanRef.
     */
    public static BooleanRef newBooleanRef() {
        return refFactory.newBooleanRef(false);
    }

    /**
     * Creates a committed BooleanRef with the provided value using the GlobalStmInstance.
     *
     * @param value the initial value
     * @return the created BooleanRef.
     */
    public static BooleanRef newBooleanRef(boolean value) {
        return refFactory.newBooleanRef(value);
    }

    /**
     * Creates a committed Ref with null as initial value using the GlobalStmInstance.
     *
     * @param <E> the type of the Ref.
     * @return the created Ref.
     */
    public static <E> Ref<E> newRef() {
        return refFactory.newRef(null);
    }

    /**
     * Creates a committed Ref with the provided value using the GlobalStmInstance.
     *
     * @param value the initial value of the Ref.
     * @param <E>   the type of the Ref.
     * @return the created Ref.
     */
    public static <E> Ref<E> newRef(E value) {
        return refFactory.newRef(value);
    }

    /**
     * Does a retry. This behavior is needed for blocking transactions; transaction that wait for a state change
     * to happen on certain datastructures, e.g. an item to come available on a transactional blocking queue.
     * <p/>
     * Under the hood the retry throws an Retry that will be caught up the callstack
     * (by the {@link AtomicBlock for example). The Retry should not be caught by user code in 99% procent
     * of the cases.
     */
    public static void retry() {
        Transaction tx = getRequiredThreadLocalTransaction();
        tx.retry();
    }

    /**
     * Prepares the Transaction in the ThreadLocalTransaction transaction.
     * <p/>
     * For more information see {@link Transaction#prepare()}.
     *
     * @throws TransactionRequiredException if no active transaction is found.
     * @throws IllegalTransactionStateException if the active transaction is not in the correct
     *                                           state for this operation.
     * @throws ControlFlowError
     */
    public static void prepare() {
        Transaction tx = getRequiredThreadLocalTransaction();
        tx.prepare();
    }

    /**
     * Aborts the Transaction in the ThreadLocalTransaction transaction.
     * <p/>
     * For more information see {@link Transaction#abort()}.
     *
     * @throws TransactionRequiredException if no active transaction is found.
     * @throws IllegalTransactionStateException if the active transaction is not in the correct
     *                                           state for this operation.
     * @throws ControlFlowError
     */
    public static void abort() {
        Transaction tx = getRequiredThreadLocalTransaction();
        tx.abort();
    }

    /**
     * Ensures all writes (so you get the guarantee that all writes are able to commit).
     *
     * For more information see {@link Transaction#ensureWrites()}
     *
     * @throws TransactionRequiredException if no active transaction is found.
     * @throws IllegalTransactionStateException if the active transaction is not in the correct
     *                                           state for this operation.
     * @throws ControlFlowError
     */
    public static void ensureWrites(){
        Transaction tx = getRequiredThreadLocalTransaction();
        tx.ensureWrites();
    }

    /**
     * Commits the Transaction in the ThreadLocalTransaction transaction.
     * <p/>
     * For more information see {@link Transaction#commit()}.
     *
     * @throws TransactionRequiredException if no active transaction is found.
     * @throws IllegalTransactionStateException if the active transaction is not in the correct
     *                                           state for this operation.
     * @throws ControlFlowError
     */
    public static void commit() {
        Transaction tx = getRequiredThreadLocalTransaction();
        tx.commit();
    }

    /**
     * Scheduled an deferred or compensating task on the Transaction in the ThreadLocalTransaction. This task is
     * executed after the transaction commits or aborts.
     * <p/>
     * @param task the deferred task to execute.
     * @throws NullPointerException if task is null.
     * @throws org.multiverse.api.exceptions.TransactionRequiredException
     *                              if no Transaction is getAndSet at the
     *                              {@link org.multiverse.api.ThreadLocalTransaction}.
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *                              if the transaction is not in the
     *                              correct state to accept a compensating or deferred task.
     */
    public static void scheduleCompensatingOrDeferredTask(final Runnable task) {
        if (task == null) {
            throw new NullPointerException();
        }

        Transaction tx = getRequiredThreadLocalTransaction();
        tx.register(new TransactionLifecycleListener() {
            @Override
            public void notify(Transaction tx, TransactionLifecycleEvent event) {
                if (event == TransactionLifecycleEvent.PostCommit
                        || event == TransactionLifecycleEvent.PostAbort) {
                    task.run();
                }
            }
        });
    }

    /**
     * Scheduled an deferred task on the Transaction in the ThreadLocalTransaction. This task is executed after
     * the transaction commits and one of the use cases is starting transactions.
     * <p/>
     * @param task the deferred task to execute.
     * @throws NullPointerException if task is null.
     * @throws org.multiverse.api.exceptions.TransactionRequiredException
     *                              if no Transaction is getAndSet at the
     *                              {@link org.multiverse.api.ThreadLocalTransaction}.
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *                              if the transaction is not in the
     *                              correct state to accept a deferred task.
     */
    public static void scheduleDeferredTask(final Runnable task) {
        if (task == null) {
            throw new NullPointerException();
        }

        Transaction tx = getRequiredThreadLocalTransaction();
        tx.register(new TransactionLifecycleListener() {
            @Override
            public void notify(Transaction tx, TransactionLifecycleEvent event) {
                if (event == TransactionLifecycleEvent.PostCommit) {
                    task.run();
                }
            }
        });
    }

    /**
     * Scheduled an compensating task on the Transaction in the ThreadLocalTransaction. This task is executed after
     * the transaction aborts and one of the use cases is cleaning up non transaction resources like the file system.
     * <p/>
     * @param task the deferred task to execute.
     * @throws NullPointerException if task is null.
     * @throws org.multiverse.api.exceptions.TransactionRequiredException
     *                              if no Transaction is getAndSet at the
     *                              {@link org.multiverse.api.ThreadLocalTransaction}.
     * @throws org.multiverse.api.exceptions.IllegalTransactionStateException
     *                              if the transaction is not in the
     *                              correct state to accept a compensating task.
     */
    public static void scheduleCompensatingTask(final Runnable task) {
        if (task == null) {
            throw new NullPointerException();
        }

        Transaction tx = getRequiredThreadLocalTransaction();
        tx.register(new TransactionLifecycleListener() {
            @Override
            public void notify(Transaction tx, TransactionLifecycleEvent event) {
                if (event == TransactionLifecycleEvent.PostAbort) {
                    task.run();
                }
            }
        });
    }

    //we don't want instances

    private StmUtils() {
    }
}
