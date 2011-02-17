package org.multiverse.api;

import org.multiverse.*;
import org.multiverse.api.closures.*;

/**
 * An AtomicBlock is responsible for executing an atomic closure. It is created by the {@link TransactionFactoryBuilder}
 * and this gives the {@link Stm} the opportunity to return different implementations based on the
 * {@link TransactionFactory} configuration. And it also gives the opportunity to provide Stm specific transaction handling
 * mechanism. In the Multiverse 0.6 design and before, a single TransactionTemplate implementation was used that should
 * be used by all Stm's, but that design is limiting.
 *
 * <p>Another useful features of this design is that for certain primitives it doesn't require any form of boxing.
 * It also provides an execute for a AtomicVoidClosure which doesn't force a developer to return something when
 * nothing needs to be returned.
 *
 * <h3>AtomicClosure</h3>
 *
 * <p>The AtomicClosure is the functionality that needs to be executed isolated, consistent and atomically. There
 * are different tasted of Closures but essentially the only difference is the return type. There are primitive closures
 * that prevent unwanted autoboxing and there also is a {@link org.multiverse.api.closures.AtomicVoidClosureVoidClosure} that prevents
 * returning a value if none is needed.
 *
 * <h3>Automatic retries</h3>
 *
 * <p>If a transaction encounters a {@link org.multiverse.api.exceptions.ReadWriteConflict} or a
 * { @link org.multiverse.api.exceptions.SpeculativeConfigurationError} it will automatically retry the
 * the AtomicClosure until either the next execution completes or the maximum number of retries has been reached.
 * To prevent contention, also a {@link BackoffPolicy} is used, to prevent transactions from causing more contention
 * if there already is contention. For configuring the maximum number of retries, see the {@link TransactionFactoryBuilder#setMaxRetries}
 * and for configuring the BackoffPolicy, see {@link TransactionFactoryBuilder#setBackoffPolicy}.
 *
 * <p>It is very important to realize that automatically retrying a transaction on a conflict is something else than the
 * {@link Transaction#retry}. The latter is really a blocking operation that only retries when there is a reason to retry.
 *
 * <h3>Thread-safety</h3>
 *
 * <p>AtomicBlocks are threadsafe. The AtomicBlock is designed to be shared between threads.
 *
 * <h3>Reuse</h3>
 *
 * <p>AtomicBlocks can be expensive to create and should be reused. Creating an AtomicBlock can lead to a lot of objects being
 * created and not reusing them leads to a lot of object waste (so put a lot of pressure on the garbage collector).
 *
 * <p>It is best to create the AtomicBlock in the beginning and store it in a (static) field and reuse it. It is very
 * unlikely that an AtomicBlock is going to be a contention point itself since in almost all cases only volatile reads are
 * required and for the rest it will be mostly immutable.
 *
 * <p>This is even more important when speculative transactions are used because speculative transactions learn on the
 * AtomicBlock level. So if the AtomicBlock is not reused, the speculative mechanism will not have full effect.
 *
 * <h3>execute vs executeChecked</h3>
 *
 * <p>The AtomicBlock provides two different types of execute methods:
 * <ol>
 * <li>execute: it will automatically wrap the checked exception that can be thrown from an AtomicClosure in a
 * {@link org.multiverse.api.exceptions.InvisibleCheckedException}. Unchecked exceptions are let through as is.
 * </li>
 * <li>execute checked: it will not do anything with thrown checked of unchecked exceptions and lets them through
 * </li>
 * </ol>
 * If an exception happens inside an AtomicClosure, the Transaction will be always aborted (unless it is caught by the logic
 * inside the AtomicClosure). Catching the exceptions inside the closure should be done with care since an exception could
 * indicate that the system has entered an invalid state.
 *
 * <p>In the future also a rollback-for functionality will be added to let a transaction commit, even though certain types
 * of exceptions have occurred. This is similar with the Spring framework where this can be configured through the
 * <a href="http://static.springsource.org/spring/docs/2.0.x/reference/transaction.html#transaction-declarative-rolling-back}">9.5.3: Rolling back</a>
 *
 * <h3>Atomic operation composition</h3>
 *
 * <p>Using traditional concurrency control, composing locking operations is extremely hard because it is very likely that
 * it is impossible without knowing implementation details of the structure, or because of deadlocks. With Stm transactional
 * operations can be composed and controlling how the system should react on existing or missing transactions can be controlled
 * through the {@link TransactionFactoryBuilder#setPropagationLevel} where the {@link PropagationLevel#Requires} is the default.
 *
 * @author Peter Veentjer.
 */
public interface AtomicBlock extends MultiverseConstants{

   /**
    * Returns the TransactionFactory that is used by this AtomicBlock to create Transactions used inside.
    *
    * @return the TransactionFactory used by this AtomicBlock.
    */
    TransactionFactory getTransactionFactory();

   /**
    * Executes the closure. If in the execution of the closure a checked exception is thrown, the exception
    * is wrapped in a InvisibleCheckedException. The original exception can be retrieved by calling the
    * getCause method.
    *
    * @param closure the closure to execute.
    * @return the result of the execution.
    * @throws NullPointerException if closure is null.
    * @throws org.multiverse.api.exceptions.InvisibleCheckedException if a checked exception is thrown by the closure.
    */
    <E> E execute(AtomicClosure<E> closure);

    /**
    * Executes the closure.
    *
    * @param closure the closure to execute.
    * @return the result of the execution.
    * @throws NullPointerException if closure is null.
    * @throws Exception if the execute call fails.
    */
    <E> E executeChecked(AtomicClosure<E> closure)throws Exception;

   /**
    * Executes the closure. If in the execution of the closure a checked exception is thrown, the exception
    * is wrapped in a InvisibleCheckedException. The original exception can be retrieved by calling the
    * getCause method.
    *
    * @param closure the closure to execute.
    * @return the result of the execution.
    * @throws NullPointerException if closure is null.
    * @throws org.multiverse.api.exceptions.InvisibleCheckedException if a checked exception is thrown by the closure.
    */
     int execute(AtomicIntClosure closure);

    /**
    * Executes the closure.
    *
    * @param closure the closure to execute.
    * @return the result of the execution.
    * @throws NullPointerException if closure is null.
    * @throws Exception if the execute call fails.
    */
     int executeChecked(AtomicIntClosure closure)throws Exception;

   /**
    * Executes the closure. If in the execution of the closure a checked exception is thrown, the exception
    * is wrapped in a InvisibleCheckedException. The original exception can be retrieved by calling the
    * getCause method.
    *
    * @param closure the closure to execute.
    * @return the result of the execution.
    * @throws NullPointerException if closure is null.
    * @throws org.multiverse.api.exceptions.InvisibleCheckedException if a checked exception is thrown by the closure.
    */
     long execute(AtomicLongClosure closure);

    /**
    * Executes the closure.
    *
    * @param closure the closure to execute.
    * @return the result of the execution.
    * @throws NullPointerException if closure is null.
    * @throws Exception if the execute call fails.
    */
     long executeChecked(AtomicLongClosure closure)throws Exception;

   /**
    * Executes the closure. If in the execution of the closure a checked exception is thrown, the exception
    * is wrapped in a InvisibleCheckedException. The original exception can be retrieved by calling the
    * getCause method.
    *
    * @param closure the closure to execute.
    * @return the result of the execution.
    * @throws NullPointerException if closure is null.
    * @throws org.multiverse.api.exceptions.InvisibleCheckedException if a checked exception is thrown by the closure.
    */
     double execute(AtomicDoubleClosure closure);

    /**
    * Executes the closure.
    *
    * @param closure the closure to execute.
    * @return the result of the execution.
    * @throws NullPointerException if closure is null.
    * @throws Exception if the execute call fails.
    */
     double executeChecked(AtomicDoubleClosure closure)throws Exception;

   /**
    * Executes the closure. If in the execution of the closure a checked exception is thrown, the exception
    * is wrapped in a InvisibleCheckedException. The original exception can be retrieved by calling the
    * getCause method.
    *
    * @param closure the closure to execute.
    * @return the result of the execution.
    * @throws NullPointerException if closure is null.
    * @throws org.multiverse.api.exceptions.InvisibleCheckedException if a checked exception is thrown by the closure.
    */
     boolean execute(AtomicBooleanClosure closure);

    /**
    * Executes the closure.
    *
    * @param closure the closure to execute.
    * @return the result of the execution.
    * @throws NullPointerException if closure is null.
    * @throws Exception if the execute call fails.
    */
     boolean executeChecked(AtomicBooleanClosure closure)throws Exception;

   /**
    * Executes the closure. If in the execution of the closure a checked exception is thrown, the exception
    * is wrapped in a InvisibleCheckedException. The original exception can be retrieved by calling the
    * getCause method.
    *
    * @param closure the closure to execute.
    * @throws NullPointerException if closure is null.
    * @throws org.multiverse.api.exceptions.InvisibleCheckedException if a checked exception is thrown by the closure.
    */
     void execute(AtomicVoidClosure closure);

    /**
    * Executes the closure.
    *
    * @param closure the closure to execute.
    * @throws NullPointerException if closure is null.
    * @throws Exception if the execute call fails.
    */
     void executeChecked(AtomicVoidClosure closure)throws Exception;

}
