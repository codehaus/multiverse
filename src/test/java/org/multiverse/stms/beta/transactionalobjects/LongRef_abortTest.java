package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmUtils;
import org.multiverse.stms.beta.orec.Orec;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class LongRef_abortTest {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void whenOpenedForRead() {
        BetaLongRef ref = BetaStmUtils.createLongRef(stm);
        Orec orec = ref.___getOrec();

        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal tranlocal = tx.openForRead(ref, false, pool);

        ref.___abort(tx, tranlocal, pool);

        assertUnlocked(orec);
        assertSurplus(0, orec);
        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenOpenedForWrite() {
        BetaTransactionalObject ref = BetaStmUtils.createLongRef(stm);
        Orec orec = ref.___getOrec();

        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal tranlocal = tx.openForWrite(ref, false, pool);

        ref.___abort(tx, tranlocal, pool);

        assertUnlocked(orec);
        assertSurplus(0, orec);
        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenLockedBySelfAndOpenedForRead() {
        BetaLongRef ref = BetaStmUtils.createLongRef(stm);

        Orec orec = ref.___getOrec();

        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal tranlocal = tx.openForRead(ref, false, pool);
        ref.___tryLockAndCheckConflict(tx, 1, tranlocal);

        ref.___abort(tx, tranlocal, pool);

        assertUnlocked(orec);
        assertSurplus(0, orec);
        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenLockedByOtherAndOpenedForRead() {
        BetaLongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        LongRefTranlocal read2 = tx.openForRead(ref, false, pool);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        LongRefTranlocal read1 = otherTx.openForRead(ref, true, pool);

        ref.___abort(tx, read2, pool);

        assertSame(committed, ref.___unsafeLoad());
        assertLocked(ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenLockedBySelfAndOpenedForWrite() {
        BetaLongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, true, pool);

        ref.___abort(tx, write, pool);

        assertSame(committed, ref.___unsafeLoad());
        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(0, ref);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenLockedByOtherAndOpenedForWrite() {
        BetaLongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        LongRefTranlocal read1 = otherTx.openForRead(ref, true, pool);

        ref.___abort(tx, write, pool);

        assertSame(committed, ref.___unsafeLoad());
        assertLocked(ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
    }
}
