package org.multiverse.stms.beta.exampletransactionalobjects;

import org.multiverse.functions.Function;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.transactionalobjects.BetaTransactionalObject;
import org.multiverse.stms.beta.transactionalobjects.Tranlocal;

public class DequeTranlocal extends Tranlocal {
    
    public DequeTranlocal(BetaTransactionalObject owner) {
        super(owner, false);
    }

    @Override
    public void prepareForPooling(BetaObjectPool pool) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public DequeTranlocal openForWrite(BetaObjectPool pool) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public DequeTranlocal openForCommute(BetaObjectPool pool) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean calculateIsDirty() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void evaluateCommutingFunctions(BetaObjectPool pool) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addCommutingFunction(Function function, BetaObjectPool pool) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
