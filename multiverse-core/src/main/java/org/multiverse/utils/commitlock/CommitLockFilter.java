package org.multiverse.utils.commitlock;

public interface CommitLockFilter {

    boolean needsLocking(CommitLock commitLock);
}
