package org.multiverse.stms.beta;

/**
 * @author Peter Veentjer
 */
public class NormalBetaTranlocal extends BetaTranlocal {

    public NormalBetaTranlocal ___origin;

    public final NormalBetaAtomicObject ___atomicObject;

    public int value;

    public NormalBetaTranlocal(NormalBetaAtomicObject atomicObject) {
        if (atomicObject == null) {
            throw new NullPointerException();
        }
        this.___atomicObject = atomicObject;
    }

    private NormalBetaTranlocal(NormalBetaTranlocal origin) {
        if (origin.___version <= 0) {
            throw new RuntimeException();
        }

        ___origin = origin;
        ___atomicObject = ___origin.___atomicObject;
        value = origin.value;
    }

    public BetaTranlocal cloneForUpdate() {
        return new NormalBetaTranlocal(this);
    }

    @Override
    public BetaTranlocalStatus getStatus() {
        if (___version > 0) {
            return BetaTranlocalStatus.readonly;
        }

        NormalBetaTranlocal actual = (NormalBetaTranlocal) ___atomicObject.load();
        if (actual == null) {
            return BetaTranlocalStatus.fresh;
        }

        if (___origin == null || ___origin != actual) {
            return BetaTranlocalStatus.conflict;
        }

        if (___origin.value != value) {
            return BetaTranlocalStatus.dirty;
        }

        return BetaTranlocalStatus.clean;
    }
}
