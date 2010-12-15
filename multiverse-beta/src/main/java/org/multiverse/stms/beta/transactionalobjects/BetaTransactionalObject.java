package org.multiverse.stms.beta.transactionalobjects;

import org.multiverse.api.TransactionalObject;
import org.multiverse.api.blocking.RetryLatch;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.Listeners;
import org.multiverse.stms.beta.orec.Orec;
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

    /**
     * Returns the Orec that belongs to this TransactionalObject.
     *
     * @return the Orec that belongs to this TransactionalObject.
     */
    Orec ___getOrec();

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
}
