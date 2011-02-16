package org.multiverse.api;

import org.multiverse.MultiverseConstants;
import org.multiverse.api.closures.*;

/**
 * An AtomicBlock is responsible for executing an atomic closure. It is created by the {@link TransactionFactoryBuilder}
 * and this gives the {@link Stm} the opportunity to return different implementations based on the
 * {@link TransactionFactory} configuration. And it also gives the opportunity to provide custom transaction handling
 * mechanism. In the Multiverse 0.6 design and before, a single TransactionTemplate implementation was used that should
 * be used by all Stm's, but that design is limiting.
 *
 * <p>Another useful features of this design is that for certain primitives it doesn't require any form of boxing.
 * It also provides an execute for a AtomicVoidClosure which doesn't force a developer to return something when
 * nothing needs to be returned.
 *
 * <h3>Threadsafe</h3>
 *
 * <p>AtomicBlocks are threadsafe.
 *
 * <h3>Reuse</h3>
 *
 * <p>AtomicBlocks can be expensive to create and should be reused. Creating an AtomicBlock can lead to a lot of objects being
 * created and not reusing them leads to a lot of object waste (so put a lot of pressure on the garbage collector).
 *
 * <p>It is best to create the AtomicBlocks in the beginning and store them in a (static) field and reuse them. It is very
 * unlikely that an AtomicBlock is going to be a contention point itself since in almost all cases only volatile reads are
 * required.
 *
 * <p>This is even more important when speculative transactions are used because speculative transactions learn on the
 * AtomicBlock level. So if the AtomicBlock is not reused, the speculative mechanism will not have full effect.
 *
 * @author Peter Veentjer.
 */
public interface AtomicBlock extends MultiverseConstants {

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
     * @throws org.multiverse.api.exceptions.InvisibleCheckedException
     *                              if a checked exception is thrown by the closure.
     */
    <E> E execute(AtomicClosure<E> closure);

    /**
     * Executes the closure.
     *
     * @param closure the closure to execute.
     * @return the result of the execution.
     * @throws NullPointerException if closure is null.
     * @throws Exception            if the execute call fails.
     */
    <E> E executeChecked(AtomicClosure<E> closure) throws Exception;

    /**
     * Executes the closure. If in the execution of the closure a checked exception is thrown, the exception
     * is wrapped in a InvisibleCheckedException. The original exception can be retrieved by calling the
     * getCause method.
     *
     * @param closure the closure to execute.
     * @return the result of the execution.
     * @throws NullPointerException if closure is null.
     * @throws org.multiverse.api.exceptions.InvisibleCheckedException
     *                              if a checked exception is thrown by the closure.
     */
    int execute(AtomicIntClosure closure);

    /**
     * Executes the closure.
     *
     * @param closure the closure to execute.
     * @return the result of the execution.
     * @throws NullPointerException if closure is null.
     * @throws Exception            if the execute call fails.
     */
    int executeChecked(AtomicIntClosure closure) throws Exception;

    /**
     * Executes the closure. If in the execution of the closure a checked exception is thrown, the exception
     * is wrapped in a InvisibleCheckedException. The original exception can be retrieved by calling the
     * getCause method.
     *
     * @param closure the closure to execute.
     * @return the result of the execution.
     * @throws NullPointerException if closure is null.
     * @throws org.multiverse.api.exceptions.InvisibleCheckedException
     *                              if a checked exception is thrown by the closure.
     */
    long execute(AtomicLongClosure closure);

    /**
     * Executes the closure.
     *
     * @param closure the closure to execute.
     * @return the result of the execution.
     * @throws NullPointerException if closure is null.
     * @throws Exception            if the execute call fails.
     */
    long executeChecked(AtomicLongClosure closure) throws Exception;

    /**
     * Executes the closure. If in the execution of the closure a checked exception is thrown, the exception
     * is wrapped in a InvisibleCheckedException. The original exception can be retrieved by calling the
     * getCause method.
     *
     * @param closure the closure to execute.
     * @return the result of the execution.
     * @throws NullPointerException if closure is null.
     * @throws org.multiverse.api.exceptions.InvisibleCheckedException
     *                              if a checked exception is thrown by the closure.
     */
    double execute(AtomicDoubleClosure closure);

    /**
     * Executes the closure.
     *
     * @param closure the closure to execute.
     * @return the result of the execution.
     * @throws NullPointerException if closure is null.
     * @throws Exception            if the execute call fails.
     */
    double executeChecked(AtomicDoubleClosure closure) throws Exception;

    /**
     * Executes the closure. If in the execution of the closure a checked exception is thrown, the exception
     * is wrapped in a InvisibleCheckedException. The original exception can be retrieved by calling the
     * getCause method.
     *
     * @param closure the closure to execute.
     * @return the result of the execution.
     * @throws NullPointerException if closure is null.
     * @throws org.multiverse.api.exceptions.InvisibleCheckedException
     *                              if a checked exception is thrown by the closure.
     */
    boolean execute(AtomicBooleanClosure closure);

    /**
     * Executes the closure.
     *
     * @param closure the closure to execute.
     * @return the result of the execution.
     * @throws NullPointerException if closure is null.
     * @throws Exception            if the execute call fails.
     */
    boolean executeChecked(AtomicBooleanClosure closure) throws Exception;

    /**
     * Executes the closure. If in the execution of the closure a checked exception is thrown, the exception
     * is wrapped in a InvisibleCheckedException. The original exception can be retrieved by calling the
     * getCause method.
     *
     * @param closure the closure to execute.
     * @throws NullPointerException if closure is null.
     * @throws org.multiverse.api.exceptions.InvisibleCheckedException
     *                              if a checked exception is thrown by the closure.
     */
    void execute(AtomicVoidClosure closure);

    /**
     * Executes the closure.
     *
     * @param closure the closure to execute.
     * @throws NullPointerException if closure is null.
     * @throws Exception            if the execute call fails.
     */
    void executeChecked(AtomicVoidClosure closure) throws Exception;

}
