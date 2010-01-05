package org.multiverse.stms.alpha.manualinstrumentation;

import org.multiverse.stms.alpha.AlphaTransactionalObject;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTranlocalSnapshot;
import org.multiverse.stms.alpha.manualinstrumentation.IntStackTranlocal.IntNode;

public final class IntStackTranlocal extends AlphaTranlocal {

    IntStack ___txObject;
    IntStackTranlocal ___origin;
    int size;
    IntNode head;

    IntStackTranlocal(IntStackTranlocal origin) {
        this.___origin = origin;
        this.___txObject = origin.___txObject;
        this.size = origin.size;
        this.head = origin.head;
    }

    IntStackTranlocal(IntStack txObject) {
        this.___txObject = txObject;
    }

    @Override
    public AlphaTranlocal openForWrite() {
        return new IntStackTranlocal(this);
    }

    @Override
    public AlphaTransactionalObject getTransactionalObject() {
        return ___txObject;
    }

    @Override
    public AlphaTranlocal getOrigin() {
        return ___origin;
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
    public boolean isDirty() {
        if (isCommitted()) {
            return false;
        }

        if(___origin == null){
            return true;
        }

        if(___origin.head!=head){
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