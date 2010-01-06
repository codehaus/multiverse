package org.multiverse.stms;

import org.multiverse.MultiverseConstants;
import org.multiverse.api.ScheduleType;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionStatus;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PanicError;
import org.multiverse.utils.latches.Latch;
import org.multiverse.utils.restartbackoff.RestartBackoffPolicy;

import static java.text.MessageFormat.format;

/**
 * An abstract {@link Transaction} implementation that contains most of the plumbing logic. Extend this and prevent
 * duplicate logic.
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
public abstract class AbstractTransaction<D extends AbstractTransactionDependencies>
        implements Transaction, MultiverseConstants {

    protected final D dependencies;
    protected final String familyName;

    private TaskListNode scheduledTasks;

    /**
     * Contains the version but also contains encoded state: version>0: started version==0 -> transaction is aborted
     * version<0 transaction is committed, and the commit version is now stored in version ( has to be multiplied by
     * -1). Long.MIN_VALUE is free
     */
    private long version;

    public AbstractTransaction(D dependencies, String familyName) {
        assert dependencies != null;
        this.dependencies = dependencies;
        this.familyName = familyName;

        if (SANITY_CHECKS_ENABLED) {
            if (dependencies.clock.getTime() == 0) {
                throw new PanicError("Clock.time has to be larger than 0, smaller version have special meaning");
            }
        }
    }

    @Override
    public long getReadVersion() {
        return version;
    }

    @Override
    public TransactionStatus getStatus() {
        if (version > 0) {
            return TransactionStatus.active;
        } else if (version == 0) {
            return TransactionStatus.aborted;
        } else {
            return TransactionStatus.committed;
        }
    }

    @Override
    public RestartBackoffPolicy getRestartBackoffPolicy() {
        return dependencies.restartBackoffPolicy;
    }

    @Override
    public String getFamilyName() {
        return familyName;
    }


    protected final void init() {
        this.scheduledTasks = null;
        this.version = dependencies.clock.getTime();
        doInit();
    }

    protected void doInit() {
    }

    @Override
    public void schedule(Runnable task, ScheduleType scheduleType) {
        switch (getStatus()) {
            case active:
                if (task == null) {
                    throw new NullPointerException();
                }

                if (scheduleType == null) {
                    throw new NullPointerException();
                }

                scheduledTasks = new TaskListNode(task, scheduleType, scheduledTasks);
                break;
            case committed:
                throw new DeadTransactionException(
                        format("Can't execute compensating task on already committed transaction '%s'", familyName));
            case aborted:
                throw new DeadTransactionException(
                        format("Can't execute compensating task on already aborted transaction '%s'", familyName));
            default:
                throw new RuntimeException();
        }
    }

    @Override
    public Transaction abortAndReturnRestarted() {
        switch (getStatus()) {
            case active:
                //fall through
            case committed:
                //fall through
            case aborted:
                init();
                return this;
            default:
                throw new RuntimeException();
        }
    }

    protected void excuteScheduledTasks(ScheduleType scheduleType) {
        if (scheduledTasks != null) {
            scheduledTasks.executeAll(scheduleType);
        }
    }

    @Override
    public void abort() {
        switch (getStatus()) {
            case active:
                try {
                    excuteScheduledTasks(ScheduleType.preAbort);
                    version = 0;//sets the status to aborted
                    doAbort();
                    excuteScheduledTasks(ScheduleType.postAbort);
                } finally {
                    scheduledTasks = null;
                }
                break;
            case committed:
                throw new DeadTransactionException(
                        format("Can't abort already committed transaction '%s'", familyName));
            case aborted:
                //ignore
                break;
            default:
                throw new RuntimeException();
        }
    }

    protected void doAbort() {
    }

    @Override
    public long commit() {
        switch (getStatus()) {
            case active:
                try {
                    excuteScheduledTasks(ScheduleType.preCommit);
                    boolean abort = true;
                    try {
                        long result = onCommit();
                        if (SANITY_CHECKS_ENABLED) {
                            if (result < 1) {
                                throw new PanicError();
                            }
                        }
                        this.version = -result;
                        abort = false;
                        excuteScheduledTasks(ScheduleType.postCommit);
                        return -version;
                    } finally {
                        if (abort) {
                            version = 0;//sets the status to aborted
                            doAbort();
                        }
                    }
                } finally {
                    scheduledTasks = null;
                }
            case committed:
                return -version;
            case aborted:
                throw new DeadTransactionException(
                        format("Can't commit already aborted transaction '%s'", familyName));
            default:
                throw new IllegalStateException();
        }
    }

    protected long onCommit() {
        return version;
    }

    @Override
    public void abortAndRegisterRetryLatch(Latch latch) {
        switch (getStatus()) {
            case active:
                if (latch == null) {
                    throw new NullPointerException();
                }
                try {
                    doAbortAndRegisterRetryLatch(latch);
                } finally {
                    version = 0;//sets the status to aborted
                }
                break;
            case committed:
                throw new DeadTransactionException(
                        format("Can't call abortAndRegisterRetryLatch on already committed transaction '%s'",
                               familyName));
            case aborted: {
                throw new DeadTransactionException(
                        format("Can't call abortAndRegisterRetryLatch on already aborted transaction '%s'",
                               familyName));
            }
            default:
                throw new RuntimeException();
        }
    }

    /**
     * Default implementation does the actual abort and then throws an {@link UnsupportedOperationException}.
     *
     * @param latch the Latch to register.
     * @throws UnsupportedOperationException depends on the implementation. But the default implementation will always
     *                                       throw this exception.
     */
    protected void doAbortAndRegisterRetryLatch(Latch latch) {
        doAbort();
        throw new UnsupportedOperationException();
    }

    @Override
    public void startOr() {
        switch (getStatus()) {
            case active:
                doStartOr();
                break;
            case committed:
                throw new DeadTransactionException(
                        format("Can't call startOr on already committed transaction '%s'", familyName));
            case aborted:
                throw new DeadTransactionException(
                        format("Can't call startOr on already aborted transaction '%s'", familyName));
            default:
                throw new RuntimeException();
        }
    }

    protected void doStartOr() {
    }

    @Override
    public void endOr() {
        switch (getStatus()) {
            case active:
                doEndOr();
                break;
            case committed:
                throw new DeadTransactionException(
                        format("Can't call endOr on already committed transaction '%s'", familyName));
            case aborted:
                throw new DeadTransactionException(
                        format("Can't call endOr on already aborted transaction '%s'", familyName));
            default:
                throw new RuntimeException();
        }
    }

    protected void doEndOr() {
    }

    @Override
    public void endOrAndStartElse() {
        switch (getStatus()) {
            case active:
                doEndOrAndStartElse();
                break;
            case committed:
                throw new DeadTransactionException(
                        format("Can't call endOrAndStartElse on already committed transaction '%s'", familyName));
            case aborted:
                throw new DeadTransactionException(
                        format("Can't call endOrAndStartElse on already aborted transaction '%s'", familyName));
            default:
                throw new RuntimeException();
        }
    }

    protected void doEndOrAndStartElse() {
    }

    private static class TaskListNode {

        private final Runnable task;
        private final ScheduleType scheduleType;
        private final TaskListNode next;

        private TaskListNode(Runnable task, ScheduleType scheduleType, TaskListNode next) {
            assert task != null;
            assert scheduleType != null;

            this.task = task;
            this.scheduleType = scheduleType;
            this.next = next;
        }

        public void executeAll(ScheduleType requiredScheduleType) {
            assert requiredScheduleType != null;

            TaskListNode node = this;
            do {
                if (scheduleType == requiredScheduleType) {
                    node.task.run();
                }
                node = node.next;
            } while (node != null);
        }
    }
}
