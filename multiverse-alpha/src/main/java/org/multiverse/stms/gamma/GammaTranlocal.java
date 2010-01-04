package org.multiverse.stms.gamma;

public class GammaTranlocal {

    public GammaTranlocal ___origin;
    public long ___version = 0;

    public int value;

    private GammaTranlocal(GammaTranlocal origin) {
        this.___origin = origin;
        this.value = origin.value;
    }

    public GammaTranlocal() {
        this.___origin = null;
    }

    public GammaTranlocal makeUpdatableClone() {
        return new GammaTranlocal(this);
    }
}
