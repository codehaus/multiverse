package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.LockMode;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmTestUtils.*;
import static org.multiverse.stms.beta.transactionalobjects.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class BetaLongRef_abortTest implements BetaStmConstants {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenOpenedForRead() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaTranlocal tranlocal = tx.openForRead(ref, LOCKMODE_NONE);

        ref.___abort(tx, tranlocal, pool);

        assertRefHasNoLocks(ref);
        assertSurplus(0, ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenOpenedForWrite() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaTranlocal tranlocal = tx.openForWrite(ref, LOCKMODE_NONE);

        ref.___abort(tx, tranlocal, pool);

        assertRefHasNoLocks(ref);
        assertSurplus(0, ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenEnsuredBySelf() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaTranlocal tranlocal = tx.openForRead(ref, LOCKMODE_WRITE);

        ref.___abort(tx, tranlocal, pool);

        assertRefHasNoLocks(ref);
        assertSurplus(0, ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenPrivatizedBySelf() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaTranlocal tranlocal = tx.openForRead(ref, LOCKMODE_EXCLUSIVE);

        ref.___abort(tx, tranlocal, pool);

        assertRefHasNoLocks(ref);
        assertSurplus(0, ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenEnsuredByOtherAndOpenedForRead() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRefTranlocal tranlocal = tx.openForRead(ref, LOCKMODE_NONE);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Write);

        ref.___abort(tx, tranlocal, pool);

        assertRefHasWriteLock(ref, otherTx);
        assertSurplus(1, ref);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenPrivatizedByOtherAndOpenedForRead() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRefTranlocal tranlocal = tx.openForRead(ref, LOCKMODE_NONE);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Exclusive);

        ref.___abort(tx, tranlocal, pool);

        assertRefHasCommitLock(ref, otherTx);

        assertSurplus(1, ref);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenPrivatizedBySelfAndOpenedForWrite() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRefTranlocal tranlocal = tx.openForWrite(ref, LOCKMODE_EXCLUSIVE);
        ref.___abort(tx, tranlocal, pool);

        assertRefHasNoLocks(ref);
        assertSurplus(0, ref);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenEnsuredBySelfAndOpenedForWrite() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRefTranlocal tranlocal = tx.openForWrite(ref, LOCKMODE_WRITE);

        ref.___abort(tx, tranlocal, pool);

        assertRefHasNoLocks(ref);
        assertSurplus(0, ref);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenPrivatizedByOtherAndOpenedForWrite() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRefTranlocal tranlocal = tx.openForWrite(ref, LOCKMODE_NONE);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Exclusive);

        ref.___abort(tx, tranlocal, pool);

        assertRefHasCommitLock(ref, otherTx);
        assertSurplus(1, ref);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }
}
