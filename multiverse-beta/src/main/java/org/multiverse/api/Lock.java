package org.multiverse.api;

/**
 * The Lock provides access to pessimistic behavior of a {@link TransactionalObject}.
 * <p/>
 * There are different types of locks:
 * <ol>
 * <li>write lock: it allows only one transaction to acquire the write lock, but unlike a traditional
 * writelock, reads still are allowed. Normally this would not be acceptable because once the writelock
 * is acquired, the internals are modified. But in case of STM, the STM can still provide a stable view
 * even though the internals could be dirty. This essentially is the same behavior you get with the
 * 'select for update' from Oracle.
 * <li>
 * <li>commit lock: it allows only one transaction to acquire the commit lock, and readers are not
 * allowed to read anymore.<li>
 * </ol>
 * <p/>
 * In the 0.8 release, the readlock also will be added.
 * <p/>
 * Locks atm are acquired for the remaining duration of the transaction and only will be released once
 * the transaction commits/aborts. This is essentially the same behavior you get with Oracle once a update/
 * delete/insert is done, or when the record is locked manually by executing the 'lock for update'.
 *
 * <h2>Blocking</h2>
 * Atm it isn't possible to block on a lock. So what happens is that some spinning is done and then some retries
 * in combination with backoffs. In the 0.8 release blocking will be added as well. The exact behavior will be
 * made configurable by some LockAcquisition policy.
 *
 * @author Peter Veentjer.
 */
public interface Lock {

    /**
     * Returns the current LockMode. This call doesn't look at any running transaction, it shows the actual
     * state of the Lock.
     *
     * @return the current LockMode.
     */
    LockMode atomicGetLockMode();

    void acquire(LockMode lockMode);

    void acquire(Transaction tx, LockMode lockMode);

    LockMode getLockMode();

    LockMode getLockMode(Transaction tx);

    boolean tryAcquire(LockMode lockMode);

    boolean tryAcquire(Transaction tx, LockMode lockMode);
}
