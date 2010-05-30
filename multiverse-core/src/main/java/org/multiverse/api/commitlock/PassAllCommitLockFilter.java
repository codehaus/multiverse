package org.multiverse.api.commitlock;

import org.multiverse.MultiverseConstants;

/**
 * A {@link CommitLockFilter} that says that all locks need locking.
 *
 * @author Peter Veentjer.
 */
public final class PassAllCommitLockFilter implements CommitLockFilter, MultiverseConstants {

    public final static PassAllCommitLockFilter INSTANCE = new PassAllCommitLockFilter();

    @Override
    public boolean needsLocking(CommitLock commitLock) {
        return true;
    }
}
