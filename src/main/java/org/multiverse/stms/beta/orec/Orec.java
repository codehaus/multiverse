package org.multiverse.stms.beta.orec;

import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.conflictcounters.GlobalConflictCounter;
import org.multiverse.stms.beta.transactionalobjects.BetaTransactionalObject;

/**
 * The problem with the traditional approach of this Orec, is that even if the transaction only
 * reads data, it still needs to call the acquire and release. The optimization is that after a
 * getAndSet of n arrive/departs that were only used for reading (you know when someone wants to do
 * an update if he acquires the lock), that the Orec goes to readonly mode. So each Orec tracks
 * how many readonly operations have been done on it. Once the orec has become read biased, it
 * is very important the no depart is called (only the {@link #___} can be
 * called).
 * <p/>
 * Once a Orec becomes biased to reading, the tranlocal that belongs to the Orec, can't be pooled
 * since you have no idea how many transactions are using it (the arrive/depart doesn't lead
 * to a change in the surplus).
 *
 * @author Peter Veentjer
 */
public interface Orec extends BetaStmConstants {

    /**
     * Checks if the Orec is locked for update. While it is locked for update, it still is readable (so arrives)
     * are allowed). The update lock can be acquired to prevent other threads from updating this orec.
     *
     * @return true if owned for writing, false otherwise.
     */
    boolean ___hasUpdateLock();

    /**
     * Checks if the Orec is locked for committing. Once it is locked, no arrives are allowed.  The commit
     * lock normally is acquired when the transaction is about to commit.
     *
     * @return true if the Orec is locked.
     */
    boolean ___hasCommitLock();

    /**
     * Returns the current number of surplus. Value is unspecified if Orec is biased to reading.
     *
     * @return the current surplus.
     */
    long ___getSurplus();

    /**
     * Tries to do an arrive. If the orec is locked for commit, arrive is not possible. But if it is locked
     * for update, an arrive is still possible.
     * <p/>
     * This call will also act as a barrier. So all changed made after a depart successfully is executed,
     * will be visible after this arrive is done.
     *
     * @param spinCount the maximum number of spins in the lock.
     * @return the arrive status (see BetaStmConstants).
     */
    int ___arrive(int spinCount);

    /**
     * Arrives at this orec an acquired the lock.
     * <p/>
     * This call also acts as a barrier.
     *
     * @param spinCount  the maximum number of spins when locked.
     * @param commitLock true if the commitLock should be acquired, false for the update lock.
     * @return the arrive status (see BetaStmConstants).
     */
    int ___tryLockAndArrive(int spinCount, boolean commitLock);

    /**
     * Lowers the amount of surplus (so called when a reading transaction stops using a transactional
     * object).
     * <p/>
     * If there is no surplus, and the orec becomes biased towards
     * readonly, the orec is locked and true is returned. This means that no other transactions are able to
     * access the orec.
     * <p/>
     * Biased towards reading, only happens when there is no surplus.
     * <p/>
     *
     * @throws org.multiverse.api.exceptions.PanicError
     *          if read biased.
     */
    @Deprecated
    void ___departAfterReading();

    /**
     * Departs and releases the lock.
     * <p/>
     * This method normally is called when a successful update is done.
     *
     * @param globalConflictCounter the GlobalConflictCounter that is called when a writeconflict is found
     * @param transactionalObject   the reference that is updated
     * @return the current surplus (so transactionalobject the depart is done)
     * @throws IllegalStateException if the orec is not locked.
     */
    @Deprecated
    long ___departAfterUpdateAndUnlock(
            GlobalConflictCounter globalConflictCounter, BetaTransactionalObject transactionalObject);

    /**
     *
     */
    @Deprecated
    void ___departAfterFailure();

    /**
     * Departs after failure and releases the locks (so the commit locks and the update lock).
     *
     * @return the remaining surplus
     * @throws org.multiverse.api.exceptions.PanicError
     *          if not locked, or when there is no surplus
     */
    @Deprecated
    long ___departAfterFailureAndUnlock();

    /**
     * Departs after a transaction has successfully read an orec and acquired the commit or update lock.
     *
     * @throws org.multiverse.api.exceptions.PanicError
     *          if no locks are acquired or if the orec is readbiased or if
     *          there is no surplus.
     */
    @Deprecated
    void ___departAfterReadingAndUnlock();

    /**
     * Unlocks the update lock and or commit lock. This call should be done by a transaction that did an arrive
     * an a readbiased orec.
     */
    @Deprecated
    void ___unlockByReadBiased();

   /**
     * Upgrades the updatelock to a commit lock. The call safely can be made if the commit lock
     * already is acquired.
     *
     * @throws org.multiverse.api.exceptions.PanicError if the updateLock and commitLock is not acquired 
     */
    void ___upgradeToCommitLock();

    /**
     * Tries to lock this Orec for update purposes. This automatically resets the biased to reading
     * behavior since an expected update is going to be done.
     *
     * @param spinCount  the maximum number of times to spin on the lock.
     * @param updateLock if the updateLock or commit lock should be acquired.
     * @return true if the lock was acquired successfully, false otherwise.
     * @throws org.multiverse.api.exceptions.PanicError
     *          if the surplus is 0 (a tryUpdateLock only can be done if the current
     *          transaction did an arrive).
     */
    boolean ___tryLockAfterNormalArrive(int spinCount, boolean updateLock);

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

    /**
     * Returns a String representation of the orec useful for debugging purposes.
     *
     * @return a String representation useful for debugging purposes.
     */
    String ___toOrecString();
}
