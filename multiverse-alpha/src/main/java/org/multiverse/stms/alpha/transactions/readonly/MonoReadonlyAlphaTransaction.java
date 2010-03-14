package org.multiverse.stms.alpha.transactions.readonly;

import org.multiverse.api.TransactionFactory;
import org.multiverse.api.exceptions.TransactionTooSmallError;
import org.multiverse.stms.AbstractTransactionConfig;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTransactionalObject;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.stms.alpha.transactions.OptimalSize;
import org.multiverse.utils.backoff.BackoffPolicy;
import org.multiverse.utils.clock.PrimitiveClock;
import org.multiverse.utils.latches.Latch;
import org.multiverse.utils.profiling.ProfileRepository;

/**
 * A tracking readonly transaction that is optimized for reading/tracking a single transactional object.
 *
 * @author Peter Veentjer.
 */
public class MonoReadonlyAlphaTransaction
        extends AbstractReadonlyAlphaTransaction<MonoReadonlyAlphaTransaction.Config> {

    public static class Config extends AbstractTransactionConfig {

        public final ProfileRepository profiler;
        public final OptimalSize optimalSize;

        public Config(PrimitiveClock clock, BackoffPolicy backoffPolicy,
                      String familyName, ProfileRepository profiler, int maxRetryCount,
                      boolean interruptible, OptimalSize optimalSize) {
            super(clock, backoffPolicy, familyName, true, maxRetryCount, interruptible, false, true);
            this.profiler = profiler;
            this.optimalSize = optimalSize;
        }
    }

    public static class Factory implements TransactionFactory<AlphaTransaction> {

        public final Config config;

        public Factory(Config config) {
            this.config = config;
        }

        @Override
        public AlphaTransaction start() {
            return new MonoReadonlyAlphaTransaction(config);
        }
    }

    private AlphaTranlocal attached;

    public MonoReadonlyAlphaTransaction(Config config) {
        super(config);
        init();
    }

    @Override
    protected void doClear() {
        attached = null;
    }

    @Override
    protected AlphaTranlocal findAttached(AlphaTransactionalObject txObject) {
        if (attached == null) {
            return null;
        }

        if (txObject != attached.getTransactionalObject()) {
            return null;
        }

        return attached;
    }

    @Override
    protected void attach(AlphaTranlocal tranlocal) {
        if (attached != null) {
            config.optimalSize.compareAndSet(1, 2);
            throw TransactionTooSmallError.INSTANCE;
        }

        attached = tranlocal;
    }

    @Override
    protected boolean doRegisterRetryLatch(Latch latch, long wakeupVersion) {
        if (attached == null) {
            return false;
        }

        AlphaTransactionalObject txObject = attached.getTransactionalObject();

        switch (txObject.___registerRetryListener(latch, wakeupVersion)) {
            case registered:
            case opened:
                return true;
            case noregistration:
                return true;
            default:
                throw new IllegalStateException();

        }
    }
}
