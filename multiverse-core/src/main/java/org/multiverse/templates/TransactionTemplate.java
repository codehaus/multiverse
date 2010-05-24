package org.multiverse.templates;

import org.multiverse.MultiverseConstants;
import org.multiverse.api.Stm;
import org.multiverse.api.ThreadLocalTransaction;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.exceptions.TooManyRetriesException;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;

import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

/**
 * A Template that handles the boilerplate code for transactions. A transaction will be started if none is available
 * around a section and if all goes right, the transaction is committed at the end.
 * <p/>
 * example:
 * <pre>
 * new TransactionTemplate(){
 *    Object execute(Transaction t){
 *        queue.push(1);
 *        return null;
 *    }
 * }.execute();
 * </pre>
 * <p/>
 * <h2>TransactionFactories</h2> Some of the methods of the TransactionTemplate don't need a {@link TransactionFactory}
 * and createReference read tracking update transaction by default. But if you want a better performance, you need
 * to pass your own TransactionFactory. One of the biggest reasons is that the system else is not able to do runtime
 * optimizations like selecting better transaction implementation.
 * <p/>
 * <h2>Ignoring ThreadLocalTransaction</h2> Out of the box the TransactionTemplate checks the {@link
 * ThreadLocalTransaction} for a running transaction, and if one is available, it doesn't createReference its own.
 * But in some cases you don't want to rely on a threadlocal, for example when you are not using instrumentation,
 * it is possible to totally ignore the ThreadLocalTransaction.
 * <p/>
 * <h2>Exceptions</h2> All uncaught throwable's lead to a rollback of the transaction.
 * <p/>
 * <h2>Threadsafe</h2> TransactionTemplate is thread-safe to use and can be reused. So in case of an actor, you could
 * create one instance in the beginning, and reuse the instance.
 * <p/>
 * <h2>Transaction reuse</h2>
 * The TransactionTemplate is able to reuse transactions stored in the TransactionThreadLocal that were created by the
 * same transaction family. For agents this is very useful, since each agent will repeatedly execute the same
 * transaction. This is good for performance since less objects need to be created. Nothing special needs to be
 * done to activate this behavior.
 *
 * @author Peter Veentjer
 */
public abstract class TransactionTemplate<E> implements MultiverseConstants {

    private final TransactionBoilerplate boilerplate;

    private final TransactionalCallable<E> callable = new TransactionalCallableImpl();

    /**
     * Creates a new TransactionTemplate that uses the STM stored in the GlobalStmInstance and works the the {@link
     * org.multiverse.api.ThreadLocalTransaction}. It automatically creates update transactions that are able to track
     * reads.
     * <p/>
     * This constructor should only be used for experimentation purposes. If you want something fast, you need to pass
     * in a TransactionFactory.
     */
    public TransactionTemplate() {
        this(getGlobalStmInstance());
    }

    /**
     * Creates a new TransactionTemplate using the provided STM. The transaction used is stores/retrieved from the
     * {@link org.multiverse.api.ThreadLocalTransaction}.
     * <p/>
     * It automatically creates read tracking update transactions.
     * <p/>
     * This constructor should only be used for experimentation purposes. If you want something fast, you need to pass
     * in a TransactionFactory.
     *
     * @param stm the stm to use for transactions.
     * @throws NullPointerException if stm is null.
     */
    public TransactionTemplate(Stm stm) {
        this(stm.getTransactionFactoryBuilder()
                .setReadonly(false)
                .setReadTrackingEnabled(true)
                .build());
    }

    /**
     * Creates a new TransactionTemplate with the provided TransactionFactory and that is aware of the
     * ThreadLocalTransaction.
     *
     * @param transactionFactory the TransactionFactory used to createReference Transactions.
     * @throws NullPointerException if txFactory is null.
     */
    public TransactionTemplate(TransactionFactory transactionFactory) {
        this(transactionFactory, true, true);
    }


    /**
     * Creates a new TransactionTemplate with the provided TransactionFactory.
     *
     * @param transactionFactory        the TransactionFactory used to createReference Transactions.
     * @param threadLocalAware          true if this TransactionTemplate should look at the ThreadLocalTransaction
     *                                  and publish the running transaction there.
     * @param lifecycleListenersEnabled true if the lifecycle callback methods should be enabled. Enabling them causes
     *                                  extra object creation, so if you want to squeeze out more performance, set
     *                                  this to false at the price of not having the lifecycle methods working.
     */
    public TransactionTemplate(TransactionFactory transactionFactory, boolean threadLocalAware,
                               boolean lifecycleListenersEnabled) {


        CallbackListener listener = lifecycleListenersEnabled ? new CallbackListener() : null;
        boilerplate = new TransactionBoilerplate(transactionFactory, listener, threadLocalAware);
    }

    /**
     * This is the method that needs to be implemented.
     *
     * @param tx the transaction used for this execution.
     * @return the result of the execution.
     * @throws Exception the Exception thrown
     */
    public abstract E execute(Transaction tx) throws Exception;

    /**
     * Checks if the TransactionTemplate should work together with the ThreadLocalTransaction.
     *
     * @return true if the TransactionTemplate should work together with the ThreadLocalTransaction.
     */
    public final boolean isThreadLocalAware() {
        return boilerplate.isThreadLocalAware();
    }

    /**
     * Checks if the lifecycle listeners on this TransactionTemplate have been enabled. Disabling it reduces
     * object creation.
     *
     * @return true if the lifecycle listener has been enabled, false otherwise.
     */
    public final boolean isLifecycleListenersEnabled() {
        return boilerplate.getTransactionLifecycleListener() != null;
    }

    /**
     * Returns the TransactionFactory this TransactionTemplate uses to createReference Transactions.
     *
     * @return the TransactionFactory this TransactionTemplate uses to createReference Transactions.
     */
    public final TransactionFactory getTransactionFactory() {
        return boilerplate.getTransactionFactory();
    }

    /**
     * Lifecycle method that is called when this TransactionTemplate is about to begin. It will be called once.
     * <p/>
     * If this TransactionTemplate doesn't starts its own transaction, this method won't be called.
     * <p/>
     * If an exception is thrown while executing this method, the execute and transaction will abort.
     */
    protected void onInit() {
    }

    /**
     * Lifecycle method that is called every time the transaction is started. It could be that a transaction is retried
     * (for example because of a read or write conflict or because of a blocking transaction).
     * <p/>
     * If this TransactionTemplate doesn't starts its own transaction, this method won't be called.
     * <p/>
     * If an exception is thrown while executing this method, the execute and transaction will abort.
     *
     * @param tx the Transaction that is started.
     */
    protected void onPostStart(Transaction tx) {
    }

    /**
     * Lifecycle method that is called when this TransactionTemplate wants to execute.
     * <p/>
     * If this TransactionTemplate doesn't starts its own transaction, this method won't be called.
     * <p/>
     * It could be that this method is called 0 to n times. 0 if the transaction didn't made it to the commit part, and
     * for every attempt to commit.
     * <p/>
     * If this method throws a Throwable, the transaction will not be committed but aborted instead.
     * <p/>
     * It could be that this method is called more than once because a template could be retrying the transaction.
     *
     * @param tx the Transaction that wants to commit.
     */
    protected void onPreCommit(Transaction tx) {
    }

    /**
     * Lifecycle method that is called when this TransactionTemplate has executed a commit.
     * <p/>
     * If an Throwable is thrown, the changes remain committed back but it could lead to not notifying all explicitly
     * registered  TransactionLifecycleListener.
     * <p/>
     * If this TransactionTemplate doesn't starts its own transaction, this method won't be called.
     * <p/>
     * This method will receive 1 call at the most, unless the template is reused of course.
     */
    protected void onPostCommit() {
    }

    /**
     * Transaction Lifecycle method that is called after a transaction was aborted.
     * <p/>
     * If an Error/RuntimeException is thrown, it could lead to not notifying all explicitly registered
     * TransactionLifecycleListener. But the transaction will always abort.
     * <p/>
     * If this TransactionTemplate doesn't starts its own transaction, this method won't be called.
     * <p/>
     * It could be that this method is called more than once because a template could be retrying the transaction.
     *
     * @param tx the transaction that currently is in the process of being aborted.
     */
    protected void onPreAbort(Transaction tx) {
    }

    /**
     * Transaction lifecycle method that is called before an abort is executed.
     * <p/>
     * If an Error/RuntimeException is thrown, it could lead to not notifying all explicitly registered
     * TransactionLifecycleListener.
     * <p/>
     * If this TransactionTemplate doesn't starts its own transaction, this method won't be called.
     * <p/>
     * It could be that this method is called more than once because a template could be retrying the transaction.
     */
    protected void onPostAbort() {
    }

    /**
     * Executes the template. This is the method you want to call if you are using the template.
     *
     * @return the result of the {@link #execute(org.multiverse.api.Transaction)} method.
     * @throws InvisibleCheckedException if a checked exception was thrown while executing the {@link
     *                                   #execute(org.multiverse.api.Transaction)} method.
     * @throws org.multiverse.api.exceptions.DeadTransactionException
     *                                   if the exception was explicitly aborted.
     * @throws TooManyRetriesException   if the template retried the transaction too many times. The cause of the last
     *                                   failure (also an exception) is included as cause. So you have some idea where
     *                                   to look for problems
     */
    public final E execute() {
        return boilerplate.execute(callable);
    }

    /**
     * Executes the Template and rethrows the checked exception instead of wrapping it in a InvisibleCheckedException.
     *
     * @return the result
     * @throws Exception               the Exception thrown inside the {@link #execute(org.multiverse.api.Transaction)}
     *                                 method.
     * @throws org.multiverse.api.exceptions.DeadTransactionException
     *                                 if the exception was explicitly aborted.
     * @throws TooManyRetriesException if the template retried the transaction too many times. The cause of the last
     *                                 failure (also an exception) is included as cause. So you have some idea where to
     *                                 look for problems
     */
    public final E executeChecked() throws Exception {
        return boilerplate.executeChecked(callable);
    }

    private class CallbackListener implements TransactionLifecycleListener {

        @Override
        public void notify(Transaction tx, TransactionLifecycleEvent event) {
            switch (event) {
                case PreStart:
                    //ignore
                    break;
                case PostStart:
                    onPostStart(tx);
                    break;
                case PrePrepare:
                    //ignore
                    break;
                case PostPrepare:
                    //ignore
                    break;
                case PreAbort:
                    onPreAbort(tx);
                    break;
                case PostAbort:
                    onPostAbort();
                    break;
                case PreCommit:
                    onPreCommit(tx);
                    break;
                case PostCommit:
                    onPostCommit();
                    break;
                default:
                    throw new IllegalStateException("unhandled event: " + event);
            }
        }
    }

    class TransactionalCallableImpl implements TransactionalCallable<E> {

        @Override
        public E call(Transaction tx) throws Exception {
            return execute(tx);
        }
    }
}
