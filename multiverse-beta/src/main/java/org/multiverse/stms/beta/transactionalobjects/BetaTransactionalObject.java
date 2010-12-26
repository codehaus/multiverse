package org.multiverse.stms.beta.transactionalobjects;

import org.multiverse.api.TransactionalObject;
import org.multiverse.api.blocking.RetryLatch;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.Listeners;
import org.multiverse.stms.beta.transactions.BetaTransaction;

/**
 * Basic interface each transactional object for the BetaStm needs to implement. When instrumentation is used,
 * this interface will be mixed in.
 * <p/>
 * All methods in this class are prefixed with '___' to prevent nameclashes when this class is mixed into
 * existing classes using instrumentation.
 *
 * @author Peter Veentjer
 */
public interface BetaTransactionalObject extends TransactionalObject {

    int VERSION_UNCOMMITTED = 0;

    /**
     * Gets the index that uniquely identifies this class. This index can be used to in arrays to collect
     * information about classes of transactional objects.
     *
     * @return the index that uniquely identifies this class
     */
    int ___getClassIndex();

    long getVersion();

    BetaTranlocal ___newTranlocal();

    /**
     * Loads the active value. If the value already is locked, by another this call will return a tranlocal
     * with the locked flag getAndSet. This call can safely be done if already locked by self. It will never return
     * null.
     *
     * @param spinCount    the number of times to spin when locked.
     * @param newLockOwner
     * @param lockMode
     * @param tranlocal
     * @return true if it was a success, false otherwise.
     */
    boolean ___load(int spinCount, BetaTransaction newLockOwner, int lockMode, BetaTranlocal tranlocal);

    /**
     * Tries to acquire the lock and checks for conflict. It is safe to call this method when it already is
     * is locked by the newLockOwner. This is useful for pessimistic locking where the locks are acquired
     * up front.
     * <p/>
     * Call should only be made when the transaction already did an arrive (so did a load).
     *
     * @param newLockOwner the transaction that want to become the lock owner.
     * @param spinCount    the maximum number of times to spin on the lock when it is locked.
     * @param tranlocal    the tranlocal used for conflict detection.
     * @param commitLock   true if the commitLock should be acquired, false if the updatelock should be acquired
     * @return true if locking was a success, false otherwise.
     */
    boolean ___tryLockAndCheckConflict(
            BetaTransaction newLockOwner, int spinCount, BetaTranlocal tranlocal, boolean commitLock);

    /**
     * Returns the transaction that currently owns the lock. If no transaction owns the lock, null
     * is returned. This call is not thread-safe.
     *
     * @return the transaction that currently owns the lock.
     */
    BetaTransaction ___getLockOwner();

    /**
     * Checks if the tranlocal has a read conflict. There is a readconflict when the current value is
     * different or when it is locked (since a conflicting write could be pending). Once a transactional
     * object is locked, its value is undetermined.
     *
     * @param tranlocal the BetaTranlocal to check if there is a read conflict
     * @return true if there was a readconflict, false otherwise.
     */
    boolean ___hasReadConflict(BetaTranlocal tranlocal);

    /**
     * Commits the all the dirty changes. The call also needs to be done when the tranlocal is readonly and
     * not permanent and locked; so that the lock is released and the departs are done.
     *
     * @param tranlocal the BetaTranlocal to commit. It doesn't matter if this is just a readonly
     *                  version, since it still may have a lock or
     * @param tx        transaction that does the commit
     * @param pool      the BetaObjectPool to use to pool the replaced tranlocal if possible.
     * @return the listeners that should be notified after the transaction completes. Value could be null,
     *         if no listeners need to be notified.
     */
    Listeners ___commitDirty(
            BetaTranlocal tranlocal, BetaTransaction tx, BetaObjectPool pool);

    Listeners ___commitAll(
            BetaTranlocal tranlocal, BetaTransaction tx, BetaObjectPool pool);

    /**
     * Aborts this BetaTransactionalObject (so releases the lock if acquired, does departs etc).
     *
     * @param transaction the transaction that wants to ___abort. This is needed to figure out if the
     *                    resource is locked by that transaction.
     * @param tranlocal   the tranlocal currently read/written
     * @param pool        the BetaObjectPool
     */
    void ___abort(BetaTransaction transaction, BetaTranlocal tranlocal, BetaObjectPool pool);

    /**
     * Registers a change listener (needed for blocking operations).
     *
     * @param latch     the Latch that gets opened when the change happens
     * @param tranlocal the current read/written tranlocal of the transaction. It is needed to determine
     *                  if a potentially desired write already has happened.
     * @param pool      the BetaObjectPool for pooling listeners
     * @param lockEra   the era of the lock when it when it 'started'. LockEra is needed for lock pooling.
     * @return true if there already is write has happened an no further registration is needed.
     */
    int ___registerChangeListener(RetryLatch latch, BetaTranlocal tranlocal, BetaObjectPool pool, long lockEra);

    /**
     * Returns the identity hash of this object. Once calculated it should be cached so that it
     * doesn't need to be calculated every time needed.
     *
     * @return the identity hashcode.
     */
    int ___identityHashCode();

    boolean ___hasLock();

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
    void ___departAfterReading();

    /**
     * Departs and releases the lock.
     * <p/>
     * This method normally is called when a successful update is done.
     *
     * @return the current surplus (so transactionalobject the depart is done)
     * @throws IllegalStateException if the orec is not locked.
     */
    long ___departAfterUpdateAndUnlock();

    /**
     *
     */
    void ___departAfterFailure();

    /**
     * Departs after failure and releases the locks (so the commit locks and the update lock).
     *
     * @return the remaining surplus
     * @throws org.multiverse.api.exceptions.PanicError
     *          if not locked, or when there is no surplus
     */
    long ___departAfterFailureAndUnlock();

    /**
     * Departs after a transaction has successfully read an orec and acquired the commit or update lock.
     *
     * @throws org.multiverse.api.exceptions.PanicError
     *          if no locks are acquired or if the orec is readbiased or if
     *          there is no surplus.
     */
    void ___departAfterReadingAndUnlock();

    /**
     * Unlocks the update lock and or commit lock. This call should be done by a transaction that did an arrive
     * an a readbiased orec.
     */
    void ___unlockByReadBiased();

    /**
     * Upgrades the updatelock to a commit lock. The call safely can be made if the commit lock
     * already is acquired.
     *
     * @throws org.multiverse.api.exceptions.PanicError
     *          if the updateLock and commitLock is not acquired
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
