package org.multiverse.stms.alpha.transactions.readonly;

import org.multiverse.api.exceptions.SpeculativeConfigurationFailure;
import org.multiverse.api.latches.Latch;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTransactionalObject;

import static java.lang.System.arraycopy;

/**
 * A readonly transaction that does readtracking, and stores all reads in an array. For short transaction
 * and array is much faster for looking up then a map.
 *
 * @author Peter Veentjer.
 */
public final class ArrayReadonlyAlphaTransaction extends AbstractReadonlyAlphaTransaction {

    private AlphaTranlocal[] attachedArray;

    private int firstFreeIndex;

    public ArrayReadonlyAlphaTransaction(ReadonlyConfiguration config, int size) {
        super(config);
        attachedArray = new AlphaTranlocal[size];
    }

    @Override
    protected void doReset() {
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
            config.speculativeConfiguration.signalSpeculativeSizeFailure(attachedArray.length);

            if (attachedArray.length >= config.speculativeConfiguration.getMaximumArraySize()) {
                throw SpeculativeConfigurationFailure.create();
            }

            AlphaTranlocal[] newAttachedArray = new AlphaTranlocal[newOptimalSize];
            arraycopy(attachedArray, 0, newAttachedArray, 0, attachedArray.length);
            attachedArray = newAttachedArray;
        }

        attachedArray[firstFreeIndex] = tranlocal;
        firstFreeIndex++;
    }

    @Override
    protected boolean dodoRegisterRetryLatch(Latch latch, long wakeupVersion) {
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
