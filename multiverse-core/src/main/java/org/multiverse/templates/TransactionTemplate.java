package org.multiverse.templates;

import org.multiverse.api.*;
import org.multiverse.api.exceptions.*;
import org.multiverse.utils.backoff.BackoffPolicy;
import org.multiverse.utils.latches.CheapLatch;

import static java.lang.String.format;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

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
 * and create read tracking update transaction by default. But if you want a better performance, you need to pass your
 * own TransactionFactory. One of the biggest reasons is that the system else is not able to do runtime optimizations
 * like selecting better transaction implementation.
 * <p/>
 * <h2>Ignoring ThreadLocalTransaction</h2> Out of the box the TransactionTemplate checks the {@link
 * ThreadLocalTransaction} for a running transaction, and if one is available, it doesn't create its own. But in some
 * cases you don't want to rely on a threadlocal, for example when you are not using instrumentation, it is possible to
 * totally ignore the ThreadLocalTransaction.
 * <p/>
 * <h2>Exceptions</h2> All uncaught throwable's lead to a rollback of the transaction.
 * <p/>
 * <h2>Threadsafe</h2> AtomicTemplates are thread-safe to use and can be reused.
 *
 * @author Peter Veentjer
 */
public abstract class TransactionTemplate<E> {

    private final boolean threadLocalAware;

    private final boolean lifecycleListenersEnabled;

    private final TransactionFactory txFactory;

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
     * Creates a new TransactionTemplate using the provided stm. The transaction used is stores/retrieved from the
     * {@link org.multiverse.api.ThreadLocalTransaction}.
     * <p/>
     * It automatically created read tracking update transactions.
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
                .setAutomaticReadTracking(true).build());
    }

    /**
     * Creates a new TransactionTemplate with the provided TransactionFactory and that is aware of the
     * ThreadLocalTransaction.
     *
     * @param txFactory the TransactionFactory used to create Transactions.
     * @throws NullPointerException if txFactory is null.
     */
    public TransactionTemplate(TransactionFactory txFactory) {
        this(txFactory, true, true);
    }

    /**
     * Creates a new TransactionTemplate with the provided TransactionFactory.
     *
     * @param txFactory                 the TransactionFactory used to create Transactions.
     * @param threadLocalAware          true if this TransactionTemplate should look at the ThreadLocalTransaction and publish
     *                                  the running transaction there.
     * @param lifecycleListenersEnabled true if the lifecycle callback methods should be enabled. Enabling them causes
     *                                  extra object creation, so if you want to squeeze out more performance, set this to false at the price of
     *                                  not having the lifecycle methods working.
     */
    public TransactionTemplate(TransactionFactory txFactory, boolean threadLocalAware, boolean lifecycleListenersEnabled) {
        if (txFactory == null) {
            throw new NullPointerException();
        }

        this.lifecycleListenersEnabled = lifecycleListenersEnabled;
        this.txFactory = txFactory;
        this.threadLocalAware = threadLocalAware;
    }

    /**
     * Checks if the TransactionTemplate should work together with the ThreadLocalTransaction.
     *
     * @return true if the TransactionTemplate should work together with the ThreadLocalTransaction.
     */
    public final boolean isThreadLocalAware() {
        return threadLocalAware;
    }

    /**
     * Checks if the lifecycle listeners on this TransactionTemplate have been enabled. Disabling it reduces
     * object creation.
     *
     * @return true if the lifecycle listener has been enabled, false otherwise.
     */
    public final boolean isLifecycleListenersEnabled() {
        return lifecycleListenersEnabled;
    }

    /**
     * Returns the TransactionFactory this TransactionTemplate uses to create Transactions.
     *
     * @return the TransactionFactory this TransactionTemplate uses to create Transactions.
     */
    public final TransactionFactory getTransactionFactory() {
        return txFactory;
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
    protected void onStart(Transaction tx) {
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
        try {
            return executeChecked();
        } catch (Exception ex) {
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else {
                throw new TransactionTemplate.InvisibleCheckedException(ex);
            }
        }
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
        Transaction tx = threadLocalAware ? getThreadLocalTransaction() : null;
        if (noActiveTransaction(tx)) {
            return executeWithTransaction();
        } else {
            return execute(tx);
        }
    }

    private E executeWithTransaction() throws Exception {
        CallbackListener listener = lifecycleListenersEnabled ? new CallbackListener() : null;

        Transaction tx = txFactory.start();
        if (threadLocalAware) {
            setThreadLocalTransaction(tx);
        }

        int attempt = 0;
        Throwable lastFailureCause = null;

        onInit();

        try {
            do {
                attempt++;
                try {
                    if (listener != null) {
                        tx.registerLifecycleListener(listener);
                    }
                    onStart(tx);
                    E result = execute(tx);
                    tx.commit();
                    return result;
                } catch (RetryError er) {
                    if (attempt - 1 < tx.getConfig().getMaxRetryCount()) {
                        awaitChangeAndRestart(tx);
                    }
                } catch (TransactionTooSmallException tooSmallException) {
                    tx = txFactory.start();
                    if (threadLocalAware) {
                        setThreadLocalTransaction(tx);
                    }
                } catch (Throwable throwable) {
                    lastFailureCause = throwable;
                    if (throwable instanceof RecoverableThrowable) {
                        BackoffPolicy backoffPolicy = tx.getConfig().getBackoffPolicy();
                        backoffPolicy.delayedUninterruptible(tx, attempt);
                        tx.restart();
                    } else {
                        rethrow(throwable);
                    }
                }
            } while (attempt - 1 < tx.getConfig().getMaxRetryCount());

            String msg = format("Too many retries on transaction '%s', maxRetryCount = %s",
                    tx.getConfig().getFamilyName(),
                    tx.getConfig().getMaxRetryCount());
            throw new TooManyRetriesException(msg, lastFailureCause);
        } finally {
            if (tx != null && tx.getStatus() != TransactionStatus.committed) {
                tx.abort();
            }

            if (threadLocalAware) {
                setThreadLocalTransaction(null);
            }
        }
    }

    private static void awaitChangeAndRestart(Transaction tx) throws InterruptedException {
        Latch latch = new CheapLatch();
        tx.registerRetryLatch(latch);
        tx.abort();
        if (tx.getConfig().isInterruptible()) {
            latch.await();
        } else {
            latch.awaitUninterruptible();
        }
        tx.restart();
    }

    private static void rethrow(Throwable ex) throws Exception {
        if (ex instanceof Exception) {
            throw (Exception) ex;
        } else if (ex instanceof Error) {
            throw (Error) ex;
        } else {
            throw new PanicError("Unthrowable throwable", ex);
        }
    }

    private boolean noActiveTransaction(Transaction t) {
        return t == null || t.getStatus() != TransactionStatus.active;
    }

    public static class InvisibleCheckedException extends RuntimeException {

        static final long serialVersionUID = 0;

        public InvisibleCheckedException(Exception cause) {
            super(cause);
        }

        @Override
        public Exception getCause() {
            return (Exception) super.getCause();
        }
    }

    private class CallbackListener implements TransactionLifecycleListener {

        @Override
        public void notify(Transaction tx, TransactionLifecycleEvent event) {
            switch (event) {
                case preAbort:
                    onPreAbort(tx);
                    break;
                case postAbort:
                    onPostAbort();
                    break;
                case preCommit:
                    onPreCommit(tx);
                    break;
                case postCommit:
                    onPostCommit();
                    break;
                default:
                    throw new IllegalStateException();
            }
        }
    }
}
