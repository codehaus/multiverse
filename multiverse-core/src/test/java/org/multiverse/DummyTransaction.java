package org.multiverse;

import org.multiverse.api.ScheduleType;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionStatus;
import org.multiverse.utils.latches.Latch;
import org.multiverse.utils.restartbackoff.RestartBackoffPolicy;

/**
 * @author Peter Veentjer
 */
public class DummyTransaction implements Transaction {

    @Override
    public RestartBackoffPolicy getRestartBackoffPolicy() {
        throw new RuntimeException();
    }

    @Override
    public String getFamilyName() {
        throw new RuntimeException();
    }


    @Override
    public void startOr() {
        throw new RuntimeException();
    }

    @Override
    public Transaction abortAndReturnRestarted() {
        throw new RuntimeException();
    }

    @Override
    public void endOr() {
        throw new RuntimeException();
    }

    @Override
    public void endOrAndStartElse() {
        throw new RuntimeException();
    }

    @Override
    public long getReadVersion() {
        throw new RuntimeException();
    }

    @Override
    public TransactionStatus getStatus() {
        throw new RuntimeException();
    }

    @Override
    public long commit() {
        throw new RuntimeException();
    }

    @Override
    public void abort() {
        throw new RuntimeException();
    }

    @Override
    public void abortAndRegisterRetryLatch(final Latch latch) {
        throw new RuntimeException();
    }

    @Override
    public void schedule(Runnable task, ScheduleType scheduleType) {
        throw new RuntimeException();
    }
}
