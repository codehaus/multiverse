package org.multiverse.stms.beta.orec;

import org.multiverse.stms.beta.BetaTransactionalObject;
import org.multiverse.stms.beta.conflictcounters.GlobalConflictCounter;

/**
 * The problem with the traditional approach of this Orec, is that even if the transaction only
 * reads data, it still needs to call the acquire and release. The optimization is that after a
 * set of n arrive/departs that were only used for reading (you know when someone wants to do
 * an update if he acquires the lock), that the Orec goes to readonly mode. So each Orec tracks
 * how many readonly operations have been done on it. Once the orec has become read biased, it
 * is very important the no depart is called (only the ___departAfterUpdateAndReleaseLock can be
 * called).
 * <p/>
 * Once a Orec becomes biased to reading, the tranlocal that belongs to the Orec, can't be pooled
 * since you have no idea how many transactions are using it (the arrive/depart doesn't lead
 * to a change in the surplus).
 *
 * @author Peter Veentjer
 */
public interface Orec {

    /**
     * Returns true if there is a surplus, false otherwise. When there is a surplus, there are transactions
     * that have read the transactional object (so have arrived), but have not left. If the Orec is read
     * biased, the surplus doesn't need to match the actual number of transactions that currently are reading
     * since the arrive and depart operations don't lead to a change in the surplus. This is done to prevent
     * unwanted contention on the Orec.
     *
     * @return true if there is a surplus, false otherwise.
     */
    boolean ___query();

    /**
     * Returns the current number of surplus. Value is unspecified if Orec is biased to reading.
     *
     * @return the current surplus.
     */
    long ___getSurplus();

    /**
     * Arrive: when the arrive is called.
     * <p/>
     * This call will also act as a barrier. So all changed made after a depart successfully is executed,
     * will be visible after this arrive is done.
     *
     * @param spinCount the maximum number of spins in the lock.
     * @return true if the arrive was a success, false if it was locked.
     */
    boolean ___arrive(int spinCount);

    /**
     * Arrives at this orec an acquired the lock.
     * <p/>
     * This call also acts as a barrier.
     *
     * @param spinCount the maximum number of spins when locked.
     * @return true if the arrive and the lock was done successfully, false otherwise.
     */
    boolean ___arriveAndLockForUpdate(int spinCount);

    /**
     * Lowers the amount of surplus.
     * <p/>
     * If there is no surplus, and the orec becomes biased towards
     * readonly, the orec is locked and true is returned. This means that no other transactions are able to
     * access the orec.
     * <p/>
     * Biased towards reading, only happens when there is no surplus.
     * <p/>
     *
     * @return true if the orec just has become read biased, false otherwise.
     * @throws org.multiverse.api.exceptions.PanicError
     *          if read biased.
     */
    boolean ___departAfterReading();

    /**
     * Departs and releases the lock.
     * <p/>
     * This method normally is called when a successful update is done.
     *
     * @param globalConflictCounter
     * @param ref
     * @return the current surplus (so after the depart is done)
     * @throws IllegalStateException if the orec is not locked.
     */
    long ___departAfterUpdateAndReleaseLock(GlobalConflictCounter globalConflictCounter, BetaTransactionalObject ref);

    /**
     *
     */
    void ___departAfterFailure();

    /**
     * Departs after failure and releases the lock.
     *
     * @return the remaining surplus
     * @throws org.multiverse.api.exceptions.PanicError
     *          if surplus is 0, or readbiased, or not locked.
     */
    long ___departAfterFailureAndReleaseLock();

    /**
     * Departs
     *
     * @return true if the orec has just become read biased, false otherwise.
     */
    boolean ___departAfterReadingAndReleaseLock();


    /**
     * Checks if the Orec is locked.
     *
     * @return true if the Orec is locked.
     */
    boolean ___isLocked();

    /**
     * Tries to lock this Orec for update purposes. This automatically resets the biased to reading
     * behavior since an expected update is going to be done.
     *
     * @param spinCount the maximum number of times to spin on the lock.
     * @return true if the lock was acquired successfully, false otherwise.
     * @throws org.multiverse.api.exceptions.PanicError
     *          if the surplus is 0 (a tryUpdateLock only can be done if the current
     *          transaction did an arrive).
     */
    boolean ___tryUpdateLock(int spinCount);

    /**
     * Releases the lock. Doesn't change the surplus or being biased to reading.  This call should only
     * be made after the {@link #___departAfterReading()} has left the orec locked.
     *
     * @throws org.multiverse.api.exceptions.PanicError
     *          if the lock is not acquired.
     */
    void ___unlockAfterBecomingReadBiased();

    /**
     * Checks if this Orec is biased towards reading.
     *
     * @return true if this Orec is biased towards reading.
     */
    boolean ___isReadBiased();

    /**
     * Returns the number of readonly operations that need to be done before being biased to
     * reading.
     *
     * @return the number of readonly operations
     */
    int ___getReadBiasedThreshold();

    /**
     * Returns the current number of consecutive readonly operations.
     * <p/>
     * Value is undetermined if the orec is biased towards reading.
     *
     * @return the current number of consecutive readonly operations.
     */
    int ___getReadonlyCount();
}
