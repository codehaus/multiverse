package org.multiverse.stms.gamma.transactionalobjects;

import org.multiverse.api.Lock;
import org.multiverse.api.blocking.RetryLatch;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaObjectPool;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.Listeners;
import org.multiverse.stms.gamma.transactions.ArrayGammaTransaction;
import org.multiverse.stms.gamma.transactions.GammaTransaction;
import org.multiverse.stms.gamma.transactions.MapGammaTransaction;
import org.multiverse.stms.gamma.transactions.MonoGammaTransaction;

public interface GammaObject extends GammaConstants {

    int VERSION_UNCOMMITTED = 0;

    Listeners safe(GammaRefTranlocal tranlocal, GammaObjectPool pool);

    GammaRefTranlocal openForWrite(GammaTransaction tx, int lockMode);

    GammaRefTranlocal openForWrite(MonoGammaTransaction tx, int lockMode);

    GammaRefTranlocal openForWrite(ArrayGammaTransaction tx, int lockMode);

    GammaRefTranlocal openForWrite(MapGammaTransaction tx, int lockMode);

    GammaRefTranlocal openForRead(GammaTransaction tx, int lockMode);

    GammaRefTranlocal openForRead(MonoGammaTransaction tx, int lockMode);

    GammaRefTranlocal openForRead(ArrayGammaTransaction tx, int lockMode);

    GammaRefTranlocal openForRead(MapGammaTransaction tx, int lockMode);

    GammaRefTranlocal openForConstruction(GammaTransaction tx);

    GammaRefTranlocal openForConstruction(MonoGammaTransaction tx);

    GammaRefTranlocal openForConstruction(MapGammaTransaction tx);

    GammaRefTranlocal openForConstruction(ArrayGammaTransaction tx);

    /**
     * Tries to acquire a lock on a previous read/written tranlocal and checks for conflict.
     * <p/>
     * Call can safely
     *
     * @param spinCount the maximum number of times to spin
     * @param tranlocal the tranlocal
     * @param lockMode
     * @return
     */
    boolean tryLockAndCheckConflict(int spinCount, GammaRefTranlocal tranlocal, int lockMode);

    boolean hasReadConflict(GammaRefTranlocal tranlocal);

    long getVersion();

    GammaStm getStm();

    Lock getLock();

    int registerChangeListener(RetryLatch latch, GammaRefTranlocal tranlocal, GammaObjectPool pool, long listenerEra);

    int identityHashCode();
}
