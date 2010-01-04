package org.multiverse.stms.alpha.transactions.readonly;

import org.multiverse.api.Latch;
import org.multiverse.api.TransactionTooSmallException;
import org.multiverse.stms.AbstractTransactionConfig;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTransactionalObject;
import org.multiverse.stms.alpha.transactions.OptimalSize;
import org.multiverse.utils.clock.Clock;
import org.multiverse.utils.profiling.ProfileRepository;
import org.multiverse.utils.restartbackoff.RestartBackoffPolicy;

import static java.lang.System.arraycopy;

public class FixedReadonlyAlphaTransaction
        extends AbstractReadonlyAlphaTransaction<FixedReadonlyAlphaTransaction.Config> {

    public static class Config extends AbstractTransactionConfig {

        public final ProfileRepository profiler;
        public final OptimalSize optimalSize;
        private int maximumSize;

        public Config(Clock clock, RestartBackoffPolicy restartBackoffPolicy,
                      String familyName, ProfileRepository profiler, int maxRetryCount, boolean interruptible,
                      OptimalSize optimalSize, int maximumSize) {
            super(clock, restartBackoffPolicy, familyName, true, maxRetryCount, interruptible, false, true);
            this.profiler = profiler;
            this.optimalSize = optimalSize;
            this.maximumSize = maximumSize;
        }
    }

    private AlphaTranlocal[] attachedArray;

    private int firstFreeIndex;

    public FixedReadonlyAlphaTransaction(Config config, int size) {
        super(config);
        attachedArray = new AlphaTranlocal[size];
        init();
    }

    @Override
    protected void doClear() {
        firstFreeIndex = 0;
        for (int k = 0; k < attachedArray.length; k++) {
            attachedArray[k] = null;
        }
    }

    @Override
    protected AlphaTranlocal findAttached(AlphaTransactionalObject txObject) {
        for (int k = 0; k < firstFreeIndex; k++) {
            AlphaTranlocal attached = attachedArray[k];
            if (attached.getTransactionalObject() == txObject) {
                return attached;
            }
        }

        return null;
    }

    @Override
    protected void attach(AlphaTranlocal tranlocal) {
        if (firstFreeIndex == attachedArray.length) {
            int newOptimalSize = attachedArray.length + 2;
            config.optimalSize.compareAndSet(attachedArray.length, attachedArray.length + 2);

            if (attachedArray.length >= config.maximumSize) {
                throw TransactionTooSmallException.INSTANCE;
            }

            AlphaTranlocal[] newAttachedArray = new AlphaTranlocal[newOptimalSize];
            arraycopy(attachedArray, 0, newAttachedArray, 0, attachedArray.length);
            attachedArray = newAttachedArray;
        }

        attachedArray[firstFreeIndex] = tranlocal;
        firstFreeIndex++;
    }

    @Override
    protected boolean doRegisterRetryLatch(Latch latch, long wakeupVersion) {
        boolean trackedReads = false;

        for (int k = 0; k < firstFreeIndex; k++) {
            AlphaTransactionalObject txObject = attachedArray[k].getTransactionalObject();
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
