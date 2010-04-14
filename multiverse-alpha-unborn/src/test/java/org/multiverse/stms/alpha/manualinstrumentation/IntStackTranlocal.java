package org.multiverse.stms.alpha.manualinstrumentation;

import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTranlocalSnapshot;
import org.multiverse.stms.alpha.manualinstrumentation.IntStackTranlocal.IntNode;

public final class IntStackTranlocal extends AlphaTranlocal {

    int size;
    IntNode head;

    IntStackTranlocal(IntStackTranlocal origin) {
        this.___origin = origin;
        this.___transactionalObject = origin.___transactionalObject;
        this.size = origin.size;
        this.head = origin.head;
    }

    IntStackTranlocal(IntStack txObject) {
        this.___transactionalObject = txObject;
    }

    @Override
    public AlphaTranlocal openForWrite() {
        return new IntStackTranlocal(this);
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
    public AlphaTranlocalSnapshot takeSnapshot() {
        return new IntStackTranlocalSnapshot(this);
    }

    @Override
    public boolean isDirty() {
        if (isCommitted()) {
            return false;
        }

        if (___origin == null) {
            return true;
        }

        IntStackTranlocal origin = (IntStackTranlocal) ___origin;
        if (origin.head != head) {
            return true;
        }

        return false;
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