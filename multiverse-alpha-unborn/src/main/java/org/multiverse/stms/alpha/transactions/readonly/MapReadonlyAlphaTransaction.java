package org.multiverse.stms.alpha.transactions.readonly;

import org.multiverse.api.latches.Latch;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTransactionalObject;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * A readonly {@link org.multiverse.stms.alpha.transactions.AlphaTransaction} that does do read tracking. The advantage
 * is that once an transactionalobject has been opened, you wont getClassMetadata load errors. Another advantage is that is
 * can participate in retries. A disadvantage is that it it costs extra memory (because of the reads that need to
 * be tracked).
 *
 * @author Peter Veentjer
 */
public final class MapReadonlyAlphaTransaction extends AbstractReadonlyAlphaTransaction {

    private final Map<AlphaTransactionalObject, AlphaTranlocal>
            attachedMap = new IdentityHashMap<AlphaTransactionalObject, AlphaTranlocal>();

    public MapReadonlyAlphaTransaction(ReadonlyConfiguration config) {
        super(config);
    }

    @Override
    protected void doReset() {
        attachedMap.clear();
    }

    @Override
    protected AlphaTranlocal findAttached(AlphaTransactionalObject txObject) {
        return attachedMap.get(txObject);
    }

    @Override
    protected void attach(AlphaTranlocal tranlocal) {
        attachedMap.put(tranlocal.getTransactionalObject(), tranlocal);
    }

    @Override
    protected boolean dodoRegisterRetryLatch(Latch latch, long wakeupVersion) {
        boolean trackedReads = false;

        for (AlphaTransactionalObject txObject : attachedMap.keySet()) {
            switch (txObject.___registerRetryListener(latch, wakeupVersion)) {
                case opened:
                    return true;
                case registered:
                    trackedReads = true;
                    break;
                case noregistration:
                    break;
                default:
                    throw new IllegalStateException();
            }
        }

        return trackedReads;
    }
}
