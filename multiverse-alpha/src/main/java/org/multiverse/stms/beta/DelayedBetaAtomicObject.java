package org.multiverse.stms.beta;

import org.multiverse.utils.TodoException;

/**
 * @author Peter Veentjer
 */
public class DelayedBetaAtomicObject extends BetaAtomicObject {

    public DelayedBetaAtomicObject(BetaStm stm) {
        //BetaTransaction transaction = stm.startUpdateTransaction((String)null);
        //transaction.load(this);
        //transaction.commit();
    }

    public DelayedBetaAtomicObject(BetaTransaction t) {
        t.load(this);
    }

    @Override
    public BetaTranlocal createInitialTranlocal() {
        return new DelayedBetaTranlocal(this, true);
    }

    public void inc(BetaStm stm) {
        //BetaTransaction t = stm.startUpdateTransaction((String)null);
        //inc(t);
        //t.commit();
    }

    public void inc(BetaTransaction t) {
        DelayedBetaTranlocal tranlocal = (DelayedBetaTranlocal) t.load(this);
        if (tranlocal.___isFixated()) {
            tranlocal.value++;
        } else {
            tranlocal.place(new IncTask());
        }
    }

    public int getValue(BetaStm stm) {
        //BetaTransaction t = stm.startUpdateTransaction((String)null);
        //int result = getValue(t);
        //t.commit();
        //return result;
        throw new TodoException();
    }

    public int getValue(BetaTransaction t) {
        DelayedBetaTranlocal tranlocal = (DelayedBetaTranlocal) t.load(this);
        tranlocal.fixate(t);
        return tranlocal.value;
    }

    class IncTask implements CommuteTask {

        @Override
        public DelayedBetaAtomicObject getAtomicObject() {
            return DelayedBetaAtomicObject.this;
        }

        @Override
        public void run(BetaTransaction t) {
            DelayedBetaTranlocal tranlocal = (DelayedBetaTranlocal) t.load(getAtomicObject());
            tranlocal.value++;
        }
    }
}
