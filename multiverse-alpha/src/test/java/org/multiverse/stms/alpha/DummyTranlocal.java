package org.multiverse.stms.alpha;

public class DummyTranlocal extends AlphaTranlocal {

    @Override
    public AlphaTranlocalSnapshot takeSnapshot() {
        throw new RuntimeException();
    }

    @Override
    public DirtinessStatus getDirtinessStatus() {
        throw new RuntimeException();
    }

    @Override
    public void prepareForCommit(long writeVersion) {
        throw new RuntimeException();
    }

    @Override
    public AlphaAtomicObject getAtomicObject() {
        throw new RuntimeException();
    }
}
