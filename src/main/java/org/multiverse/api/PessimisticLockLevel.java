package org.multiverse.api;


public enum PessimisticLockLevel {

    Write(false, true), Read(true, true), None(false, false);

    private final boolean lockReads;
    private final boolean lockWrites;

    private PessimisticLockLevel(boolean lockReads, boolean lockWrites) {
        this.lockReads = lockReads;
        this.lockWrites = lockWrites;
    }

    public final boolean lockWrites() {
        return lockWrites;
    }

    public final boolean lockReads() {
        return lockReads;
    }
}
