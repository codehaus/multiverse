package org.multiverse.api.commitlock;

public interface CommitLockFilter {

    boolean needsLocking(CommitLock commitLock);
}
