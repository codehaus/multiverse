package org.multiverse.stms.alpha.transactions.readonly;

import org.multiverse.api.exceptions.TransactionTooSmallError;
import org.multiverse.stms.AbstractTransactionConfig;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTransactionalObject;
import org.multiverse.stms.alpha.transactions.OptimalSize;
import org.multiverse.utils.backoff.BackoffPolicy;
import org.multiverse.utils.clock.PrimitiveClock;
import org.multiverse.utils.latches.Latch;

import static java.lang.System.arraycopy;

public class ArrayReadonlyAlphaTransaction
        extends AbstractReadonlyAlphaTransaction<ArrayReadonlyAlphaTransaction.Config> {

    public static class Config extends AbstractTransactionConfig {

        public final OptimalSize optimalSize;
        private int maximumSize;

        public Config(PrimitiveClock clock, BackoffPolicy backoffPolicy, String familyName, int maxRetryCount, boolean interruptible,
                      OptimalSize optimalSize, int maximumSize) {
            super(clock, backoffPolicy, familyName, true, maxRetryCount, interruptible, false, true);
            this.optimalSize = optimalSize;
            this.maximumSize = maximumSize;
        }
    }

    private AlphaTranlocal[] attachedArray;

    private int firstFreeIndex;

    public ArrayReadonlyAlphaTransaction(Config config, int size) {
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
                throw TransactionTooSmallError.INSTANCE;
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
