package org.multiverse.stms.alpha;

import org.multiverse.api.Latch;
import org.multiverse.stms.alpha.RegisterRetryListenerResult;
import org.multiverse.utils.Listeners;
import org.multiverse.utils.commitlock.CommitLock;

/**
 * This is the interface placed on the POJO's that want to participate in the alpha STM.
 * <p/>
 * In most cases a user of the library is not going to worry bout this interface. Instrumentation is going to do all
 * that work based on annotations.
 *
 * @author Peter Veentjer.
 */
public interface AlphaTransactionalObject extends CommitLock {

    /**
     * Loads a readonly {@link AlphaTranlocal} with a version equal or smaller than readVersion. It is very important
     * for the implementation to not to return a too old version. If this happens, the system could start to suffer from
     * lost updates (not seeing changes you should have seen).
     * <p/>
     *
     * @param readVersion the version of the Tranlocal to read.
     * @return the loaded Tranlocal. If nothing is committed, null is returned.
     * @throws org.multiverse.api.exceptions.LoadException
     *          if the system wasn't able to load the Tranlocal.
     */
    AlphaTranlocal ___load(long readVersion);

    /**
     * Checks if there is a conflict. 
     *
     * @return true is there is a conflict, false if not.
     */
    boolean ___hasConflict(long version);

    /**
     * Loads the most recently committed AlphaTranlocal. Call never fails. Value could be stale as soon as it is
     * retrieved. If no commits have been made, null is returned.
     *
     * @return the most recently written AlphaTranlocal.
     */
    AlphaTranlocal ___load();

    /**
     * Creates a fresh AlphaTranlocal. This can be used when a write on a tranlocal needs to be done, but no tranlocal
     * has been created yet (so should be created in the constructor).
     *
     * @return the created AlphaTranlocal.
     */
    AlphaTranlocal ___openUnconstructed();

    /**
     * Stores the the content and releases the lock.
     * <p/>
     * It is important that this call only is made when the lock already was acquired.
     *
     * @param tranlocal    the Tranlocal to storeAndReleaseLock.
     * @param writeVersion the version to storeAndReleaseLock the Tranlocal with.
     * @return the Listeners to wake up. Could be null if there are no listeners to wake up.
     */
    Listeners ___store(AlphaTranlocal tranlocal, long writeVersion);

    /**
     * Registers a listener for retrying (the condition variable version for STM's). The Latch is a concurrency
     * structure that can be used to let a thread (transaction) wait for a specific event. In this case we use it to
     * notify the Transaction that the desired update has taken place.
     *
     * @param listener      the Latch to register.
     * @param wakeupVersion the minimum version to wake up for.
     * @return true if the listener was registered on a committed object, false otherwise.
     */
    RegisterRetryListenerResult ___registerRetryListener(Latch listener, long wakeupVersion);
}
