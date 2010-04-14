package org.multiverse.stms.alpha;

import org.multiverse.api.commitlock.CommitLock;
import org.multiverse.api.commitlock.CommitLockFilter;

/**
 * A {@link CommitLockFilter} that filters out all tranlocals that don't need locking.
 *
 * @author Peter Veentjer.
 */
public class UncommittedFilter implements CommitLockFilter {

    public final static UncommittedFilter NO_DIRTY_CHECK = new UncommittedFilter(false);

    public final static UncommittedFilter DIRTY_CHECK = new UncommittedFilter(true);

    private final boolean dirtyCheck;

    public UncommittedFilter(boolean dirtyCheck) {
        this.dirtyCheck = dirtyCheck;
    }

    @Override
    public boolean needsLocking(CommitLock commitLock) {
        AlphaTranlocal tranlocal = (AlphaTranlocal) commitLock;

        if (tranlocal.isCommitted()) {
            return false;
        }

        if (tranlocal.getOrigin() == null) {
            return false;
        }

        if (dirtyCheck) {
            return tranlocal.getPrecalculatedIsDirty();
        }

        return true;
    }
}

