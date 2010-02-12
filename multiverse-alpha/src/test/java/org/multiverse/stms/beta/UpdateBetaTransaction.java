package org.multiverse.stms.beta;

import org.multiverse.stms.AbstractTransaction;
import org.multiverse.stms.AbstractTransactionConfig;

import java.util.IdentityHashMap;
import java.util.Map;

public class UpdateBetaTransaction extends AbstractTransaction implements BetaTransaction {

    private final Map<BetaObject, BetaTranlocal> attached = new IdentityHashMap<BetaObject, BetaTranlocal>();

    public UpdateBetaTransaction(AbstractTransactionConfig config) {
        super(config);

        init();
    }

    @Override
    public BetaTranlocal openForRead(BetaObject object) {
        if (object == null) {
            return null;
        }

        BetaTranlocal tranlocal = attached.get(object);
        if (tranlocal == null) {
            tranlocal = object.___load(getReadVersion());
            attached.put(object, tranlocal);
        }

        return tranlocal;
    }

    @Override
    public BetaTranlocal openForWrite(BetaObject object) {
        if (object == null) {
            throw new NullPointerException();
        }

        BetaTranlocal tranlocal = attached.get(object);
        if (tranlocal == null) {
            BetaTranlocal active = object.___load(getReadVersion());
            if (active == null) {
                tranlocal = object.___openNew();
            } else {
                tranlocal = active.___openForWrite();
            }
            attached.put(object, tranlocal);
        } else if (tranlocal.___isCommitted()) {
            tranlocal = tranlocal.___openForWrite();
            attached.put(object, tranlocal);
        }

        return tranlocal;
    }

    @Override
    protected void doPrepare() {
        super.doPrepare();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    protected void doStore() {
        long writeVersion = config.clock.tick();

        for (BetaTranlocal tranlocal : attached.values()) {
            if (tranlocal.___isDirty()) {
                tranlocal.___store(writeVersion);
            }
        }
    }

    @Override
    protected void doClear() {
        attached.clear();
    }
}
