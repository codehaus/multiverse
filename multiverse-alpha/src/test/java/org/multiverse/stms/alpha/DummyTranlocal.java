package org.multiverse.stms.alpha;

public class DummyTranlocal extends AlphaTranlocal {

    @Override
    public AlphaTranlocalSnapshot takeSnapshot() {
        throw new RuntimeException();
    }

    @Override
    public boolean isDirty() {
        throw new RuntimeException();
    }

    @Override
    public void prepareForCommit(long writeVersion) {
        throw new RuntimeException();
    }

    @Override
    public AlphaTranlocal openForWrite() {
        throw new RuntimeException();
    }
}
