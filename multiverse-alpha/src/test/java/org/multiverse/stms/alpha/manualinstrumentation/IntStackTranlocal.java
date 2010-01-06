package org.multiverse.stms.alpha.manualinstrumentation;

import org.multiverse.stms.alpha.AlphaAtomicObject;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTranlocalSnapshot;
import org.multiverse.stms.alpha.DirtinessStatus;
import org.multiverse.stms.alpha.manualinstrumentation.IntStackTranlocal.IntNode;

public final class IntStackTranlocal extends AlphaTranlocal {

    IntStack ___atomicObject;
    IntStackTranlocal ___origin;
    int size;
    IntNode head;

    IntStackTranlocal(IntStackTranlocal origin) {
        this.___origin = origin;
        this.___atomicObject = origin.___atomicObject;
        this.size = origin.size;
        this.head = origin.head;
    }

    IntStackTranlocal(IntStack atomicObject) {
        this.___atomicObject = atomicObject;
    }

    @Override
    public AlphaAtomicObject getAtomicObject() {
        return ___atomicObject;
    }

    public static class IntNode {

        final int value;
        final IntNode next;

        IntNode(int value, IntNode next) {
            this.value = value;
            this.next = next;
        }
    }

    @Override
    public void prepareForCommit(long writeVersion) {
        this.___writeVersion = writeVersion;
        this.___origin = null;
    }

    @Override
    public AlphaTranlocalSnapshot takeSnapshot() {
        return new IntStackTranlocalSnapshot(this);
    }

    @Override
    public DirtinessStatus getDirtinessStatus() {
        if (___writeVersion > 0) {
            return DirtinessStatus.readonly;
        } else if (___origin == null) {
            return DirtinessStatus.fresh;
        } else if (___origin.head != this.head) {
            return DirtinessStatus.dirty;
        } else {
            return DirtinessStatus.clean;
        }
    }
}

final class IntStackTranlocalSnapshot extends AlphaTranlocalSnapshot {

    public final IntStackTranlocal ___tranlocal;
    public final int size;
    public final IntNode head;

    IntStackTranlocalSnapshot(IntStackTranlocal tranlocal) {
        this.___tranlocal = tranlocal;
        this.size = tranlocal.size;
        this.head = tranlocal.head;
    }

    @Override
    public AlphaTranlocal getTranlocal() {
        return ___tranlocal;
    }

    @Override
    public void restore() {
        ___tranlocal.size = size;
        ___tranlocal.head = head;
    }
}