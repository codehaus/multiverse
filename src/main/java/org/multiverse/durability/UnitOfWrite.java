package org.multiverse.durability;

/**
 * A set containing all potential changes that need to be written. The idea is that persistent roots are used,
 * and all DurableObjects reachable from a persistent root is going to be persisted. It is the tasks of the
 * Storage implementation to figure out what and what not to persist.
 * <p/>
 * A UnitOfWrite essentially is the same as a transaction where all change committed are atomic, isolated, consistent
 * etc. But a different name is chosen to prevent confusion.
 * <p/>
 * UnitOfWrite is not threadsafe.
 *
 * @author Peter Veentjer
 */
public interface UnitOfWrite {

    /**
     *
     *
     * @param id
     * @param factory
     * @return
     */
    DurableObject getOrCreate(String id, DurableObjectFactory factory);

    /**
     * Adds a new root to this UnitOfWrite.
     *
     * @param root the root to add.
     * @throws NullPointerException  if root is null.
     * @throws IllegalStateException if the UnitOfWrite is not in the active state.
     */
    void addRoot(DurableObject root);

    /**
     * Adds a change to this UnitOfWrite.
     * <p/>
     * Multiple additions of the same DurableState are ignored.
     *
     * @param change the change to add.
     * @throws NullPointerException     if change is null.
     * @throws IllegalArgumentException if there already is a different DurableState for the owner of the DurableState.
     * @throws IllegalStateException    if the UnitOfWrite is not in the active state.
     */
    void addChange(DurableState change);

    /**
     * Returns the current state of this UnitOfWrite.
     *
     * @return the current state.
     */
    UnitOfWorkState getState();

    /**
     * Commits the changes.
     * <p/>
     * The locks on all owners of the StateChange should be acquired before calling the commit. This is not the
     * responsibility of this UnitOfWrite but of the Transaction that uses it. The same goes for the roots.
     *
     * @throws IllegalStateException if the UnitOfWrite is not in the active state.
     */
    void commit();
}
