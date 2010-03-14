package org.multiverse.stms.alpha.transactions.readonly;

import org.multiverse.api.TransactionFactory;
import org.multiverse.stms.AbstractTransactionConfig;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTransactionalObject;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.utils.backoff.BackoffPolicy;
import org.multiverse.utils.clock.PrimitiveClock;
import org.multiverse.utils.latches.Latch;
import org.multiverse.utils.profiling.ProfileRepository;

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
public class MapReadonlyAlphaTransaction
        extends AbstractReadonlyAlphaTransaction<MapReadonlyAlphaTransaction.Config> {

    public static class Config extends AbstractTransactionConfig {

        public final ProfileRepository profiler;

        public Config(PrimitiveClock clock, BackoffPolicy backoffPolicy,
                      String familyName, ProfileRepository profiler, int maxRetryCount, boolean interruptible) {
            super(clock, backoffPolicy, familyName, true, maxRetryCount, interruptible, true, true);
            this.profiler = profiler;
        }
    }

    public static class Factory implements TransactionFactory<AlphaTransaction> {

        public final Config config;

        public Factory(Config config) {
            this.config = config;
        }

        @Override
        public AlphaTransaction start() {
            return new MapReadonlyAlphaTransaction(config);
        }
    }

    private final Map<AlphaTransactionalObject, AlphaTranlocal>
            attachedMap = new IdentityHashMap<AlphaTransactionalObject, AlphaTranlocal>();

    public MapReadonlyAlphaTransaction(Config config) {
        super(config);

        init();
    }

    @Override
    protected void doClear() {
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
    protected boolean doRegisterRetryLatch(Latch latch, long wakeupVersion) {
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
