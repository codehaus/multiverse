package org.multiverse.stms.beta.transactionalobjects;

import org.multiverse.api.LockStatus;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.references.RefArray;

public class BetaRefArray<E> implements RefArray<E> {

    @Override
    public Stm getStm() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public LockStatus getLockStatus(Transaction tx) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
