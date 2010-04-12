package org.multiverse.stms;

import org.multiverse.MultiverseConstants;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionConfiguration;
import org.multiverse.api.TransactionStatus;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.NoRetryPossibleException;
import org.multiverse.api.latches.Latch;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;

import java.util.LinkedList;
import java.util.List;

import static java.text.MessageFormat.format;

/**
 * An abstract {@link org.multiverse.api.Transaction} implementation that contains most of the plumbing logic. Extend
 * this and prevent duplicate logic.
 * <p/>
 * The do-methods can be overridden.
 * <p/>
 * The subclass needs to call the {@link #init()} when it has completed its constructor. Can't be done inside the
 * constructor of the AbstractTransaction because fields in the subclass perhaps are not set.
 * <p/>
 * AbstractTransaction requires the clock.time to be at least 1. It used the version field to encode the transaction
 * state. See the version field for more information.
 *
 * @author Peter Veentjer.
 */
public abstract class AbstractTransaction<C extends AbstractTransactionConfiguration, S extends AbstractTransactionSnapshot>
        implements Transaction, MultiverseConstants {

    protected final C config;

    private List<TransactionLifecycleListener> listeners;

    private long version;

    private TransactionStatus status;

    public AbstractTransaction(C config) {
        assert config != null;
        this.config = config;
    }

    @Override
    public long getReadVersion() {
        return version;
    }

    @Override
    public final TransactionStatus getStatus() {
        return status;
    }

    @Override
    public TransactionConfiguration getConfiguration() {
        return config;
    }

    protected final void init() {
        status = TransactionStatus.active;
        version = config.clock.getVersion();
        doInit();
    }

    /**
     * Method is designed to be overridden to add custom behavior on the init of the transaction.
     */
    protected void doInit() {
    }

    protected void doClear() {
    }

    @Override
    public final void registerLifecycleListener(TransactionLifecycleListener listener) {
        switch (status) {
            case active:
                //fall through
            case prepared:
                if (listener == null) {
                    throw new NullPointerException();
                }

                if (listeners == null) {
                    listeners = new LinkedList<TransactionLifecycleListener>();
                }
                listeners.add(listener);
                break;
            case committed:
                String committedMsg = format("Can't register TransactionLifecycleListener on already committed " +
                        "transaction '%s'",
                        config.getFamilyName());
                throw new DeadTransactionException(committedMsg);
            case aborted:
                String abortMsg = format("Can't register TransactionLifecycleListener on already aborted transaction '%s'",
                        config.getFamilyName());
                throw new DeadTransactionException(abortMsg);
            default:
                throw new RuntimeException();
        }
    }

    @Override
    public final void prepare() {
        switch (status) {
            case active:
                try {
                    notifyAll(TransactionLifecycleEvent.preCommit);

                    //todo: the pre-commit tasks need to be executed
                    doPrepare();
                    status = TransactionStatus.prepared;
                } finally {
                    if (status != TransactionStatus.prepared) {
                        abort();
                    }
                }
                break;
            case prepared:
                //ignore
                break;
            case committed:
                String committedMsg = format("Can't prepare already committed transaction '%s'",
                        config.getFamilyName());
                throw new DeadTransactionException(committedMsg);
            case aborted:
                String abortedMsg = format("Can't prepare already aborted transaction '%s'",
                        config.getFamilyName());
                throw new DeadTransactionException(abortedMsg);
            default:
                throw new RuntimeException();
        }
    }

    protected void doPrepare() {
    }

    @Override
    public final void restart() {
        switch (status) {
            case active:
                //fall through
            case prepared:
                abort();
                //fall through
            case committed:
                //fall through
            case aborted:
                clearListeners();
                doClear();
                init();
                break;
            default:
                throw new RuntimeException();
        }
    }

    private void clearListeners() {
        if (listeners != null) {
            listeners.clear();
        }
    }

    /**
     * Executes the ScheduledTasks for the specific event. The tasks field is not changed. If the tasks fields is null,
     * the call is ignored. If a task fails, the other tasks won't be executed and the thrown error will be propagated.
     *
     * @param event the TransactionLifecycleEvent to execute the tasks for.
     */
    private void notifyAll(TransactionLifecycleEvent event) {
        if (listeners == null) {
            return;
        }

        for (TransactionLifecycleListener listener : listeners) {
            listener.notify(this, event);
        }
    }

    @Override
    public final void abort() {
        switch (status) {
            case active:
                //fall through
            case prepared:
                try {
                    try {
                        notifyAll(TransactionLifecycleEvent.preAbort);
                    } finally {
                        status = TransactionStatus.aborted;
                        if (status == TransactionStatus.active) {
                            doAbortActive();
                        } else {
                            doAbortPrepared();
                        }
                    }
                    notifyAll(TransactionLifecycleEvent.postAbort);
                } finally {
                    clearListeners();
                }
                break;
            case committed:
                String committedMsg = format("Can't abort already committed transaction '%s'", config.getFamilyName());
                throw new DeadTransactionException(committedMsg);
            case aborted:
                //ignore
                break;
            default:
                throw new RuntimeException();
        }
    }

    protected void doAbortPrepared() {
    }

    /**
     * Method is designed to be overridden to add custom behavior on the abort of the transaction.
     */
    protected void doAbortActive() {
    }

    @Override
    public final void commit() {
        switch (status) {
            case active:
                prepare();
                //fall through
            case prepared:
                try {
                    makeChangesPermanent();
                    status = TransactionStatus.committed;
                    notifyAll(TransactionLifecycleEvent.postCommit);
                } finally {
                    if (status != TransactionStatus.committed) {
                        abort();
                    }
                }
                break;
            case committed:
                //ignore the call
                return;
            case aborted:
                String abortedMsg = format("Can't commit already aborted transaction '%s'", config.getFamilyName());
                throw new DeadTransactionException(abortedMsg);
            default:
                throw new IllegalStateException();
        }
    }

    protected void makeChangesPermanent() {
    }

    @Override
    public final void registerRetryLatch(Latch latch) {
        switch (status) {
            case active:
                //fall through
            case prepared:
                if (latch == null) {
                    throw new NullPointerException();
                }

                boolean success = doRegisterRetryLatch(latch, version + 1);

                if (!success) {
                    String msg = format("No retry is possible on transaction '%s' because it has no tracked reads, "
                            + "or not all reads are known (because it doesn''t support isAutomaticReadTrackingEnabled) ",
                            config.getFamilyName());
                    throw new NoRetryPossibleException(msg);
                }
                break;
            case committed:
                String commitMsg = format("No retry is possible on already committed transaction '%s'",
                        config.getFamilyName());
                throw new DeadTransactionException(commitMsg);
            case aborted: {
                String abortedMsg = format("No retry is possible on already aborted transaction '%s'",
                        config.getFamilyName());
                throw new DeadTransactionException(abortedMsg);
            }
            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Register retry. Default implementation returns 0, indicating that a retry is not possible.
     * <p/>
     * This method is designed to be overridden.
     *
     * @param latch         the latch to registerLifecycleListener. Value will never be null.
     * @param wakeupVersion the minimal version of the transactional object to wakeup for.
     * @return true if there were tracked reads (so the latch was opened or registered at at least 1 transactional
     *         object)
     */
    protected boolean doRegisterRetryLatch(Latch latch, long wakeupVersion) {
        return false;
    }

    public final void startOr() {
        switch (status) {
            case active:
                S snapshot = takeSnapshot();
                snapshot.parent = getSnapshot();
                snapshot.tasks = null;//listeners;
                storeSnapshot(snapshot);
                break;
            case committed:
                String commitMsg = format("Can't call startOr on already committed transaction '%s'",
                        config.getFamilyName());
                throw new DeadTransactionException(commitMsg);
            case aborted:
                String abortMsg = format("Can't call startOr on already aborted transaction '%s'",
                        config.getFamilyName());
                throw new DeadTransactionException(abortMsg);
            default:
                throw new RuntimeException();
        }
    }

    public final void endOr() {
        switch (status) {
            case active:
                S snapshot = getSnapshot();
                if (snapshot == null) {
                    throw new IllegalStateException();//improve exception
                }

                storeSnapshot((S) snapshot.parent);
                break;
            case committed:
                String committedMsg = format("Can't call endOr on already committed transaction '%s'",
                        config.getFamilyName());
                throw new DeadTransactionException(committedMsg);
            case aborted:
                String abortedMsg = format("Can't call endOr on already aborted transaction '%s'",
                        config.getFamilyName());
                throw new DeadTransactionException(abortedMsg);
            default:
                throw new RuntimeException();
        }
    }

    public final void endOrAndStartElse() {
        switch (status) {
            case active:
                S snapshot = getSnapshot();
                if (snapshot == null) {
                    throw new IllegalStateException();
                }

                //listeners = snapshot.tasks;
                snapshot.restore();
                storeSnapshot((S) snapshot.parent);
                break;
            case committed:
                String commitMsg = format("Can't call endOrAndStartElse on already committed transaction '%s'",
                        config.getFamilyName());
                throw new DeadTransactionException(commitMsg);
            case aborted:
                String abortMsg = format("Can't call endOrAndStartElse on already aborted transaction '%s'",
                        config.getFamilyName());
                throw new DeadTransactionException(abortMsg);
            default:
                throw new RuntimeException();
        }
    }

    protected S takeSnapshot() {
        throw new UnsupportedOperationException();
    }

    protected S getSnapshot() {
        throw new UnsupportedOperationException();
    }

    protected void storeSnapshot(S snapshot) {
        throw new UnsupportedOperationException();
    }
}
