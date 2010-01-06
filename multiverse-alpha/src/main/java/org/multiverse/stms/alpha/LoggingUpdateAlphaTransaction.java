package org.multiverse.stms.alpha;

import org.multiverse.api.ScheduleType;
import org.multiverse.api.Transaction;
import static org.multiverse.stms.alpha.AlphaStmUtils.toAtomicObjectString;
import org.multiverse.utils.latches.Latch;

import static java.lang.String.format;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A logging version of the {@link org.multiverse.stms.alpha.UpdateAlphaTransaction}.
 *
 * @author Peter Veentjer.
 */
public class LoggingUpdateAlphaTransaction extends UpdateAlphaTransaction {

    private final static Logger logger = Logger.getLogger(UpdateAlphaTransaction.class.getName());

    private final long logId;
    private final Level level;

    public LoggingUpdateAlphaTransaction(UpdateTransactionDependencies dependencies, String familyName, long logId,
                                         Level level) {
        super(dependencies, familyName);
        this.logId = logId;
        this.level = level;

        if (logger.isLoggable(level)) {
            logger.log(level, format("%s started", toLogString()));
        }
    }


    private String toLogString() {
        return format(
                "UpdateTransaction '%s-%s' with readversion '%s' ",
                getFamilyName(),
                logId,
                getReadVersion());
    }

    @Override
    public AlphaTranlocal load(AlphaAtomicObject atomicObject) {
        if (!logger.isLoggable(level)) {
            return super.load(atomicObject);
        } else {
            boolean success = false;
            try {
                AlphaTranlocal tranlocal = super.load(atomicObject);
                success = true;
                return tranlocal;
            } finally {
                if (success) {
                    String msg = format("%s load %s", toLogString(), toAtomicObjectString(atomicObject));
                    logger.log(level, msg);
                } else {
                    String msg = format("%s load %s failed", toLogString(), toAtomicObjectString(atomicObject));
                    logger.log(level, msg);
                }
            }
        }
    }

    @Override
    public long commit() {
        if (!logger.isLoggable(level)) {
            return super.commit();
        } else {
            boolean success = false;
            long version = Long.MIN_VALUE;
            try {
                version = super.commit();
                success = true;
                return version;
            } finally {
                if (success) {
                    logger.log(level, format("%s committed with version %s", toLogString(), version));
                } else {
                    logger.log(level, format("%s aborted", toLogString()));
                }
            }
        }
    }

    @Override
    public void abort() {
        if (!logger.isLoggable(level)) {
            super.abort();
        } else {
            boolean success = false;
            try {
                super.abort();
                success = true;
            } finally {
                if (success) {
                    logger.log(level, format("%s aborted", toLogString()));
                } else {
                    logger.log(level, format("%s abort failed", toLogString()));
                }
            }
        }
    }

    @Override
    public Transaction abortAndReturnRestarted() {
        if (!logger.isLoggable(level)) {
            return super.abortAndReturnRestarted();
        } else {
            boolean success = false;
            String oldLogString = toLogString();
            try {
                Transaction t = super.abortAndReturnRestarted();
                success = true;
                return t;
            } finally {
                if (success) {
                    String msg = format(
                            "%s abortAndReturnRestarted to readversion %s",
                            oldLogString,
                            getReadVersion());
                    logger.log(level, msg);
                } else {
                    logger.log(level, format("%s abortAndReturnRestarted failed", oldLogString));
                }
            }
        }
    }


    @Override
    public void abortAndRegisterRetryLatch(Latch latch) {
        if (!logger.isLoggable(level)) {
            super.abortAndRegisterRetryLatch(latch);
        } else {
            boolean success = false;
            try {
                super.abortAndRegisterRetryLatch(latch);
                success = true;
            } finally {
                if (success) {
                    logger.log(level, format("%s abortedAndRetry successfully", toLogString()));
                } else {
                    logger.log(level, format("%s abortedAndRetry failed", toLogString()));
                }
            }
        }
    }

    @Override
    public void schedule(Runnable task, ScheduleType scheduleType) {
        if (!logger.isLoggable(level)) {
            super.schedule(task, scheduleType);
        } else {
            boolean success = false;
            try {
                super.schedule(task, scheduleType);
                success = true;
            } finally {
                if (success) {
                    logger.log(level, format("%s scheduleType %s %s", toLogString(), scheduleType, task));
                } else {
                    logger.log(level, format("%s scheduleType %s %s failed", toLogString(), scheduleType, task));
                }
            }
        }
    }

    @Override
    public String toString() {
        return format("UpdateTransaction %s-%s", getFamilyName(), logId);
    }
}
