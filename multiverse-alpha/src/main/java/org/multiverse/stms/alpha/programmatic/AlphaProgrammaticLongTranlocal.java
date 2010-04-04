package org.multiverse.stms.alpha.programmatic;

import org.multiverse.api.exceptions.UncommittedReadConflict;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTranlocalSnapshot;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.utils.TodoException;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticLongTranlocal extends AlphaTranlocal {

    public long value;
    public long pendingIncrements;

    public AlphaProgrammaticLongTranlocal(AlphaProgrammaticLong transactionalObject, boolean commuting) {
        this.___transactionalObject = transactionalObject;
        this.___writeVersion = commuting ? -2 : 0;
    }

    public AlphaProgrammaticLongTranlocal(AlphaProgrammaticLongTranlocal origin) {
        this.___origin = origin;
        this.___transactionalObject = origin.___transactionalObject;
        this.value = origin.value;
    }

    @Override
    public void fixatePremature(AlphaTransaction tx, AlphaTranlocal origin) {
        if (!isCommuting()) {
            return;
        }

        //System.out.println("premature fixation");

        this.___origin = origin;
        this.value = ((AlphaProgrammaticLongTranlocal) origin).value;
        this.value += pendingIncrements;
        this.pendingIncrements = 0;
        //-1 indicates that it is a normaly dirty that needs writing.
        this.___writeVersion = -1;
    }

    @Override
    public void ifCommutingThenFixate(AlphaTransaction tx) {
        if (!isCommuting()) {
            return;
        }

        AlphaProgrammaticLongTranlocal tranlocal = (AlphaProgrammaticLongTranlocal) ___transactionalObject.___load();
        if (tranlocal == null) {
            throw new UncommittedReadConflict();
        }

        //System.out.println("late fixation");

        this.value = tranlocal.value;
        this.value += pendingIncrements;
        this.pendingIncrements = 0;

        //-1 indicates that it is a normaly dirty that needs writing.
        this.___writeVersion = -1;
    }

    @Override
    public AlphaTranlocalSnapshot takeSnapshot() {
        throw new TodoException();
    }

    @Override
    public AlphaTranlocal openForWrite() {
        return new AlphaProgrammaticLongTranlocal(this);
    }

    @Override
    public boolean isDirty() {
        if (isCommitted()) {
            return false;
        }

        if (___origin == null) {
            return true;
        }

        if (isCommuting()) {
            return pendingIncrements != 0;
        }

        AlphaProgrammaticLongTranlocal org = (AlphaProgrammaticLongTranlocal) ___origin;
        return org.value != value;
    }
}
