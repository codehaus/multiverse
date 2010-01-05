package org.multiverse.stms.alpha;

/**
 * A snapshot of a {@link AlphaTranlocal}. This can be used to make a snapshot and restore an Tranlocal to that state
 * later when the transaction does a rollback. This functionality is useful for the orelse mechanism that requires
 * rollback on Tranlocals.
 * <p/>
 * A TranlocalSnapshot also is a single linked list. This is done to prevent object creation overhead.
 *
 * @author Peter Veentjer.
 */
public abstract class AlphaTranlocalSnapshot {

    /**
     * Each {@link AlphaTranlocalSnapshot} can also be used as a single linked list. Normally you would not add this
     * kind of junk to your objects, but for performance reasons we make an exception. Object creation is very expensive
     * en should be reduced to a minimum.
     */
    public AlphaTranlocalSnapshot ___next;

    /**
     * The Tranlocal that created this TranlocalSnapshot.
     *
     * @return the Tranlocal that created this AlphaTranlocalSnapshot.
     */
    public abstract AlphaTranlocal getTranlocal();

    /**
     * Does the rollback so that the Tranlocal is restored. This method should only be called as long as the Tranlocal
     * is not committed. No check is required to be done when it is committed.
     */
    public abstract void restore();
}
