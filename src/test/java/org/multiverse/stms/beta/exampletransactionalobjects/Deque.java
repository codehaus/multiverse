package org.multiverse.stms.beta.exampletransactionalobjects;

import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.transactionalobjects.AbstractBetaTransactionalObject;
import org.multiverse.stms.beta.transactions.BetaTransaction;

public class Deque extends AbstractBetaTransactionalObject{

    public Deque(BetaTransaction tx) {
        super(tx);
    }

    @Override
    public DequeTranlocal ___openForConstruction(BetaObjectPool pool) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public DequeTranlocal ___openForCommute(BetaObjectPool pool) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
