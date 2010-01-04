package org.multiverse.stms.delta;

/**
 * Contains the threadlocal content of an {@link DeltaAtomicObject}.
 * <p/>
 * Origin is used for read/write validation. Is set null after commit.
 * The version contains the commit version of the DeltaTranlocal. 0 indicates that
 * it hasn't been committed and a value larger than 0 indicates that it has been committed
 * and should only be used for readonly purposes.
 *
 * @author Peter Veentjer
 */
public class DeltaTranlocal {

    public DeltaTranlocal ___origin;
    public long ___version = 0;

    public int value;

    private DeltaTranlocal(DeltaTranlocal origin) {
        this.___origin = origin;
        this.value = origin.value;
    }

    public DeltaTranlocal() {
        this.___origin = null;
    }

    public DeltaTranlocal makeUpdatableClone() {
        return new DeltaTranlocal(this);
    }
}
