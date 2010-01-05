package org.multiverse.stms.beta;

/**
 * @author Peter Veentjer
 */
public class NormalBetaAtomicObject extends BetaAtomicObject {

    public NormalBetaAtomicObject(BetaTransaction t) {
        BetaTranlocal tranlocal = t.load(this);
    }

    public NormalBetaAtomicObject(BetaStm stm) {
        /*
        BetaTransaction t = stm.startUpdateTransaction((String)null);
        BetaTranlocal tranlocal = t.load(this);
        t.commit();*/
    }

    public void inc(BetaStm stm) {
        /*
        BetaTransaction t = stm.startUpdateTransaction((String)null);
        inc(t);
        t.commit();*/
    }

    public void inc(BetaTransaction t) {
        NormalBetaTranlocal tranlocal = (NormalBetaTranlocal) t.load(this);
        tranlocal.value++;
    }

    @Override
    public BetaTranlocal createInitialTranlocal() {
        return new NormalBetaTranlocal(this);
    }
}
