package org.multiverse.stms.beta;

import org.multiverse.api.blocking.Latch;
import org.multiverse.api.blocking.Listeners;
import org.multiverse.durability.DurableObject;
import org.multiverse.stms.beta.conflictcounters.GlobalConflictCounter;
import org.multiverse.stms.beta.orec.Orec;
import org.multiverse.stms.beta.refs.Tranlocal;
import org.multiverse.stms.beta.transactions.BetaTransaction;

/**
 * Basic interface each transactional object for the BetaStm needs to implement. When instrumentation is used,
 * this interface will be mixed in.
 *
 * @author Peter Veentjer
 */
public interface BetaTransactionalObject {

    /**
     * Gets the index that uniquely identifies this class. This index can be used to in arrays to collect
     * information about classes of transactional objects.
     *
     * @return the index that uniquely identifies this class
     */
    int getClassIndex();

    /**
     * Returns the Orec that belongs to this TransactionalObject.
     *
     * @return the Orec that belongs to this TransactionalObject.
     */
    Orec getOrec();

    /**
     * Opens the TransactionalObject for construction.
     *
     * @param pool the ObjectPool to use for garbage collected objects.
     * @return the opened Tranlocal.
     */
    Tranlocal openForConstruction(ObjectPool pool);

    /**
     * Loads the active value.
     *
     * @param spinCount the number of times to spin when locked.
     * @return the active value.
     */
    Tranlocal load(int spinCount);

    /**
     * Loads and locks the value. If the value already is locked, by another this call will return a tranlocal
     * with the locked flag set. This call can safely be done if already locked by self.
     *
     * @param spinCount    the maximum number of times to spin
     * @param newLockOwner  the transaction that wants to own the lock (not allowed to be null).
     * @return
     */
    Tranlocal lockAndLoad(int spinCount, BetaTransaction newLockOwner);

    /**
     * Loads the current stored Tranlocal without any form of consistency guarantees. This method is purely
     * meant for testing/debugging purposes.
     *
     * @return the current stored tranlocal.
     */
    Tranlocal unsafeLoad();

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
     * @return true if locking was a success, false otherwise.
     */
    boolean tryLockAndCheckConflict(BetaTransaction newLockOwner, int spinCount, Tranlocal tranlocal);

    /**
     * Returns the transaction that currently owns the lock. If no transaction owns the lock, null
     * is returned. This call is not thread-safe.
     *
     * @return the transaction that currently owns the lock.
     */
    BetaTransaction getLockOwner();

    /**
     * Checks if the tranlocal has a read conflict. There is a readconflict when the current value is
     * different or when it is locked (since a conflicting write could be pending). Once a transactional
     * object is locked, its value is undetermined.
     *
     * @param tranlocal  the Tranlocal to check if there is a read conflict
     * @param transaction the transaction
     * @return true if there was a readconflict, false otherwise.
     */
    boolean hasReadConflict(Tranlocal tranlocal, BetaTransaction transaction);

    void commitConstructed(Tranlocal tranlocal);

    /**
     * Commits the all the dirty changes. The call also needs to be done when the tranlocal is readonly and
     * not permanent and locked; so that the lock is released and the departs are done.
     *
     * @param tranlocal             the Tranlocal to commit. It doesn't matter if this is just a readonly
     *                              version, since it still may have a lock or
     * @param tx                    transaction that does the commit
     * @param pool                  the ObjectPool to use to pool the replaced tranlocal if possible.
     * @param globalConflictCounter
     * @return the listeners that should be notified after the transaction completes. Value could be null,
     * if no listeners need to be notified.
     */
    Listeners commitDirty(
            Tranlocal tranlocal, BetaTransaction tx, ObjectPool pool, GlobalConflictCounter globalConflictCounter);

    /**
     * Commits the all updates. The call also needs to be done when the tranlocal is readonly and
     * not permanent and locked; so that the lock is released and the departs are done.
     *
     * @param tranlocal             the Tranlocal to commit. It doesn't matter if this is just a readonly
     *                              version, since it still may have a lock or
     * @param tx                    transaction that does the commit
     * @param pool                  the ObjectPool to use to pool the replaced tranlocal if possible.
     * @param globalConflictCounter
     * @return the listeners that should be notified after the transaction completes. Value could be null,
     * if no listeners need to be notified.
     */
    Listeners commitAll(
            Tranlocal tranlocal, BetaTransaction tx, ObjectPool pool, GlobalConflictCounter globalConflictCounter);

    /**
     * Aborts this BetaTransactionalObject (so releases the lock if acquired, does departs etc).
     *
     * @param transaction
     * @param tranlocal
     * @param pool
     */
    void abort(BetaTransaction transaction, Tranlocal tranlocal, ObjectPool pool);

    /**
     * Returns the identity hash of this object. Once calculated it should be cached so that it
     * doesn't need to be calculated every time needed.
     *
     * @return the identity hashcode.
     */
    int identityHashCode();

    /**
     *
     * @param latch
     * @param tranlocal the current read/written tranlocal of the transaction. It is needed to determine
     *        if a potentially desired write already has happened.
     * @param pool   the ObjectPool for pooling listeners
     * @param lockEra
     * @return true if there already is write has happened an no further registration is needed.
     */
    boolean registerChangeListener(Latch latch, Tranlocal tranlocal, ObjectPool pool, long lockEra);
}
