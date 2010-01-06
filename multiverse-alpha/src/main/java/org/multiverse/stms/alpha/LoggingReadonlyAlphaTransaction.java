package org.multiverse.stms.alpha;

import org.multiverse.api.ScheduleType;
import org.multiverse.api.Transaction;

import static java.lang.String.format;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A logging version of the {@link org.multiverse.stms.alpha.ReadonlyAlphaTransaction}.
 *
 * @author Peter Veentjer.
 */
public class LoggingReadonlyAlphaTransaction extends ReadonlyAlphaTransaction {

    private final static Logger logger = Logger.getLogger(UpdateAlphaTransaction.class.getName());

    private final long logId;
    private final Level level;

    public LoggingReadonlyAlphaTransaction(ReadonlyAlphaTransactionDependencies dependenciesAlpha,
                                           String familyName, long logId, Level level) {
        super(dependenciesAlpha, familyName);

        this.logId = logId;
        this.level = level;

        if (logger.isLoggable(level)) {
            logger.log(level, format("%s started", toLogString()));
        }
    }

    private String toLogString() {
        return format("ReadonlyTransaction '%s-%s' with readversion '%s' ",
                      getFamilyName(),
                      logId, getReadVersion());
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
                    logger.log(level, format("%s abortAndReturnRestarted to readversion %s",
                                             oldLogString,
                                             getReadVersion()));
                } else {
                    logger.log(level, format("%s abortAndReturnRestarted failed", oldLogString));
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
                    logger.log(level, format("%s compensatingExecute %s %s", toLogString(), scheduleType, task));
                } else {
                    logger.log(level, format("%s compensatingExecute %s %s failed", toLogString(), scheduleType, task));
                }
            }
        }
    }

    @Override
    public String toString() {
        return toLogString();
    }
}
