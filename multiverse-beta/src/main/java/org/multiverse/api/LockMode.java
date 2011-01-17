package org.multiverse.api;

import org.multiverse.MultiverseConstants;

public enum LockMode implements MultiverseConstants{

    None(LOCKMODE_NONE),

    Read(LOCKMODE_READ),

    Write(LOCKMODE_WRITE),

    Exclusive(LOCKMODE_EXCLUSIVE);

    private int lockMode;

    private LockMode(int lockMode){
        this.lockMode = lockMode;
    }

    public int asInt() {
        return lockMode;
    }
}
