package org.multiverse.stms;

import org.multiverse.MultiverseConstants;
import org.multiverse.api.*;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.NoRetryPossibleException;
import org.multiverse.api.exceptions.PanicError;
import org.multiverse.utils.TodoException;

import static java.text.MessageFormat.format;

/**
 * An abstract {@link org.multiverse.api.Transaction} implementation that contains most of the plumbing logic. Extend
 * this and prevent duplicate logic.
 * <p/>
 * The on-methods can be overridden.
 * <p/>
 * The subclass needs to call the {@link #init()} when it has completed its constructor. Can't be done inside the
 * constructor of the AbstractTransaction because fields in the subclass perhaps are not set.
 * <p/>
 * AbstractTransaction requires the clock.time to be at least 1. It used the version field to encode the transaction
 * state. See the version field for more information.
 *
 * @author Peter Veentjer.
 */
public abstract class AbstractTransaction<C extends AbstractTransactionConfig, S extends AbstractTransactionSnapshot>
        implements Transaction, MultiverseConstants {

    protected final C config;

    private TransactionLifecycleListener listeners;

    private long version;

    private TransactionStatus status;

    public AbstractTransaction(C config) {
        assert config != null;
        this.config = config;

        if (___SANITY_CHECKS_ENABLED) {
            if (config.clock.getVersion() <= 0) {
                throw new PanicError("Clock.time has to be larger than 0, smaller version have special meaning");
            }
        }
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
    public TransactionConfig getConfig() {
        return config;
    }

    protected final void init() {
        status = TransactionStatus.active;
        this.version = config.clock.getVersion();
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
    public final void register(TransactionLifecycleListener listener) {
        switch (status) {
            case active:
                if (listener == null) {
                    throw new NullPointerException();
                }

                listener.next = listeners;
                listeners = listener;
                break;
            case committed:
                String committedMsg = format("Can't register listener on already committed transaction '%s'",
                        config.getFamilyName());
                throw new DeadTransactionException(committedMsg);
            case aborted:
                String abortMsg = format("Can't register listener on already aborted transaction '%s'",
                        config.getFamilyName());
                throw new DeadTransactionException(abortMsg);
            default:
                throw new RuntimeException();
        }
    }


    @Override
    public void join(TwoPhaseCommitGroup group) {
        switch (status) {
            case active:
                throw new TodoException();
            case committed:
                String committedMsg = format("Can't start 2phase commit on already committed transaction '%s'",
                        config.getFamilyName());
                throw new DeadTransactionException(committedMsg);
            case aborted:
                String abortedMsg = format("Can't start 2phase on already on already aborted transaction '%s'",
                        config.getFamilyName());
                throw new DeadTransactionException(abortedMsg);
            default:
                throw new RuntimeException();
        }
    }

    protected TwoPhaseCommitGroup doStartTwoPhaseCommit() {
        String committedMsg = format("Can't start 2phase commit transaction '%s' because it isn't supported",
                config.getFamilyName());
        throw new UnsupportedOperationException(committedMsg);
    }


    @Override
    public final void restart() {
        switch (status) {
            case active:
                abort();
                //fall through
            case committed:
                //fall through
            case aborted:
                doClear();
                init();
                break;
            default:
                throw new RuntimeException();
        }
    }

    /**
     * Executes the ScheduledTasks for the specific event. The tasks field is not changed. If the tasks fields is null,
     * the call is ignored. If a task fails, the other tasks won't be executed and the thrown error will be propagated.
     *
     * @param event the TransactionLifecycleEvent to execute the tasks for.
     */
    private void notifyAll(TransactionLifecycleEvent event) {
        TransactionLifecycleListener listener = listeners;

        while (listener != null) {
            listener.notify(this, event);
            listener = listener.next;
        }
    }

    @Override
    public final void abort() {
        switch (status) {
            case active:
                try {
                    notifyAll(TransactionLifecycleEvent.preAbort);
                    status = TransactionStatus.aborted;
                    doAbort();
                    notifyAll(TransactionLifecycleEvent.postAbort);
                } finally {
                    //we have to make sure that whatever happens, the transaction is aborted.
                    status = TransactionStatus.aborted;
                    listeners = null;
                }
                break;
            case committed:
                String committedMsg = format("Can't abort already committed transaction '%s'",
                        config.getFamilyName());
                throw new DeadTransactionException(committedMsg);
            case aborted:
                //ignore
                break;
            default:
                throw new RuntimeException();
        }
    }

    /**
     * Method is designed to be overridden to add custom behavior on the abort of the transaction.
     */
    protected void doAbort() {
    }

    @Override
    public final void commit() {
        switch (status) {
            case active:
                try {
                    try {
                        notifyAll(TransactionLifecycleEvent.preCommit);                        
                        doCommit();
                        status = TransactionStatus.committed;
                        notifyAll(TransactionLifecycleEvent.postCommit);
                    } finally {
                        if (status != TransactionStatus.committed) {
                            abort();
                        }
                    }
                } finally {
                }
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

    /**
     * Method is designed to be overridden to add custom behavior on the commit of the transaction.
     * <p/>
     * The default behavior is that the readversion is returned (so usefull for readonly transactions).
     */
    protected void doCommit() {
    }

    @Override
    public final void registerRetryLatch(Latch latch) {
        switch (status) {
            case active:
                if (latch == null) {
                    throw new NullPointerException();
                }

                boolean trackedReads = doRegisterRetryLatch(latch, version + 1);
                if (!trackedReads) {
                    String msg = format("No retry is possible on transaction '%s' because it has no tracked reads",
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
     * @param latch         the latch to register. Value will never be null.
     * @param wakeupVersion the minimal version of the transactional object to wakeup for.
     * @return true if there were tracked reads (so the latch was opened or registered at at least 1
     *         transactional object)
     */
    protected boolean doRegisterRetryLatch(Latch latch, long wakeupVersion) {
        return false;
    }

    @Override
    public final void startOr() {
        switch (status) {
            case active:
                S snapshot = takeSnapshot();
                snapshot.parent = getSnapshot();
                snapshot.tasks = listeners;
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

    @Override
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

    @Override
    public final void endOrAndStartElse() {
        switch (status) {
            case active:
                S snapshot = getSnapshot();
                if (snapshot == null) {
                    throw new IllegalStateException();//todo: improve exception
                }

                listeners = snapshot.tasks;
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
