package org.multiverse.stms.gamma.transactionalobjects;

import org.multiverse.api.Lock;
import org.multiverse.api.blocking.RetryLatch;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaObjectPool;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.Listeners;

public interface GammaObject extends GammaConstants {

    Listeners commit(GammaRefTranlocal tranlocal, GammaObjectPool pool);

    /**
     * Tries to acquire a lock on a previous read/written tranlocal and checks for conflict.
     * <p/>
     * If the lockMode == LOCKMODE_NONE, this call is ignored.
     * <p/>
     * The call to this method can safely made if the current lock level is higher the the desired LockMode.
     * <p/>
     * If the can't be acquired, no changes are made on the tranlocal.
     *
     * @param spinCount       the maximum number of times to spin
     * @param tranlocal       the tranlocal
     * @param desiredLockMode
     * @return true if the lock was acquired successfully and there was no conflict.
     */
    boolean tryLockAndCheckConflict(int spinCount, GammaRefTranlocal tranlocal, int desiredLockMode);

    boolean hasReadConflict(GammaRefTranlocal tranlocal);

    long getVersion();

    GammaStm getStm();

    Lock getLock();

    int registerChangeListener(RetryLatch latch, GammaRefTranlocal tranlocal, GammaObjectPool pool, long listenerEra);

    int identityHashCode();
}
