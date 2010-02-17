package org.multiverse.commitbarriers;

import java.util.List;

/**
 * A CountDownCommitBarrier is a blocking structure (a latch) that commits or aborts bases on the number of
 * waiting transactions. Once all the members of the barrier have returned, all the transactions are committed.
 * <p/>
 * This functionality is useful for two phase commit related functionality.
 * <p/>
 * The CountDownCommitBarrier can't be reused.
 * <p/>
 * A CountDownCommitBarrier is thread-safe to use.
 *
 * @author Peter Veentjer.
 * @see VetoCommitBarrier
 */
public final class CountDownCommitBarrier extends CommitBarrier {

    private final int parties;

    /**
     * Create a new CountDownCommitBarrier that uses an unfair lock.
     *
     * @param parties the number of parties waiting. If the number of parties is 0, the VetoCommitBarrier is created
     *                committed, else it will be closed.
     * @throws IllegalArgumentException if parties is smaller than 0.
     */
    public CountDownCommitBarrier(int parties) {
        this(parties, false);
    }

    /**
     * Creates a new CountDownCommitBarrier.
     *
     * @param parties the number of parties waiting. If the number of parties is 0, the VetoCommitBarrier is created
     *                committed, else it will be closed.
     * @param fair    if the lock bu this CountDownCommitBarrier is fair.
     * @throws IllegalArgumentException if parties smaller than 0.
     */
    public CountDownCommitBarrier(int parties, boolean fair) {
        super(parties == 0 ? Status.committed : Status.closed, fair);

        if (parties < 0) {
            throw new IllegalArgumentException();
        }

        this.parties = parties;
    }

    /**
     * Returns the number of parties that want to join this CountDownCommitBarrier.
     *
     * @return the number of parties.
     */
    public int getParties() {
        return parties;
    }

    protected boolean isLastParty() {
        return getNumberWaiting() == parties;
    }

    /**
     * @throws CommitBarrierOpenException if the CountDownCommitBarrier already is aborted or committed.
     */
    public void countDown() {
        List<Runnable> onCommitTasks = null;

        lock.lock();
        try {
            switch (getStatus()) {
                case closed:
                    addWaiter(null);

                    if (isLastParty()) {
                        onCommitTasks = signalCommit();
                    }
                    break;
                case aborted:
                    String abortMsg = "Can't call countDown on already aborted CountDownCommitBarrier";
                    throw new CommitBarrierOpenException(abortMsg);
                case committed:
                    String commitMsg = "Can't call countDown on already committed CountDownCommitBarrier";
                    throw new CommitBarrierOpenException(commitMsg);
                default:
                    throw new IllegalStateException();
            }
        } finally {
            lock.unlock();
        }

        executeTasks(onCommitTasks);
    }
}
