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

    Listeners safe(GammaTranlocal tranlocal, GammaObjectPool pool);

    GammaTranlocal openForWrite(GammaTransaction tx, int lockMode);

    GammaTranlocal openForWrite(MonoGammaTransaction tx, int lockMode);

    GammaTranlocal openForWrite(ArrayGammaTransaction tx, int lockMode);

    GammaTranlocal openForWrite(MapGammaTransaction tx, int lockMode);

    GammaTranlocal openForRead(GammaTransaction tx, int lockMode);

    GammaTranlocal openForRead(MonoGammaTransaction tx, int lockMode);

    GammaTranlocal openForRead(ArrayGammaTransaction tx, int lockMode);

    GammaTranlocal openForRead(MapGammaTransaction tx, int lockMode);

    GammaTranlocal openForConstruction(GammaTransaction tx);

    GammaTranlocal openForConstruction(MonoGammaTransaction tx);

    GammaTranlocal openForConstruction(MapGammaTransaction tx);

    GammaTranlocal openForConstruction(ArrayGammaTransaction tx);

    /**
     * Tries to acquire a lock on a previous read/written tranlocal and checks for conflict.
     *
     * Call can safely
     *
     * @param spinCount
     * @param tranlocal
     * @param lockMode
     * @return
     */
    boolean tryLockAndCheckConflict(int spinCount, GammaTranlocal tranlocal, int lockMode);

    boolean hasReadConflict(GammaTranlocal tranlocal);

    void releaseAfterFailure(GammaTranlocal tranlocal, GammaObjectPool pool);

    void releaseAfterUpdate(GammaTranlocal tranlocal, GammaObjectPool pool);

    void releaseAfterReading(GammaTranlocal tranlocal, GammaObjectPool pool);

    long getVersion();

    GammaStm getStm();

    Lock getLock();

    int registerChangeListener(RetryLatch latch, GammaTranlocal tranlocal, GammaObjectPool pool, long listenerEra);

    int identityHashCode();


}
