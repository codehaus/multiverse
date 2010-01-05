package org.multiverse.utils.commitlock;

import org.multiverse.MultiverseConstants;
import org.multiverse.api.exceptions.PanicError;

/**
 * A {@link CommitLockFilter} that says that all locks need locking.
 *
 * @author Peter Veentjer.
 */
public class PassAllCommitLockFilter implements CommitLockFilter, MultiverseConstants {

    public final static PassAllCommitLockFilter INSTANCE = new PassAllCommitLockFilter();

    @Override
    public boolean needsLocking(CommitLock commitLock) {
        if (___SANITY_CHECKS_ENABLED) {
            //make sure that the commitLock is not null.
            if (commitLock == null) {
                throw new PanicError();
            }

            //for the time being it is not allowed that a commitlock already is locked
            if (commitLock.___getLockOwner() != null) {
                throw new PanicError();
            }
        }

        return true;
    }
}
