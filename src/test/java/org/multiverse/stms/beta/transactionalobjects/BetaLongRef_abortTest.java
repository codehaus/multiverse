package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.blocking.Latch;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.multiverse.TestUtils.assertHasListeners;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmTestUtils.assertVersionAndValue;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

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
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();
        long value = ref.___weakRead();

        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal tranlocal = tx.openForRead(ref, LOCKMODE_NONE);

        ref.___abort(tx, tranlocal, pool);

        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
        assertEquals(version, ref.getVersion());
        assertEquals(value, ref.___weakRead());
    }

    @Test
    public void whenOpenedForWrite() {
        BetaLongRef ref = newLongRef(stm);
        long version = ref.getVersion();
        long value = ref.___weakRead();

        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal tranlocal = tx.openForWrite(ref, LOCKMODE_NONE);

        ref.___abort(tx, tranlocal, pool);

        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
        assertEquals(version, ref.getVersion());
        assertEquals(value, ref.___weakRead());
    }

    @Test
    public void whenEnsuredBySelf() {
        BetaLongRef ref = newLongRef(stm);
        long version = ref.getVersion();
        long value = ref.___weakRead();


        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal tranlocal = tx.openForRead(ref, LOCKMODE_UPDATE);

        ref.___abort(tx, tranlocal, pool);

        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
        assertEquals(version, ref.getVersion());
        assertEquals(value, ref.___weakRead());
    }

    @Test
    public void whenPrivatizedBySelf() {
        BetaLongRef ref = newLongRef(stm);
        long version = ref.getVersion();
        long value = ref.___weakRead();


        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal tranlocal = tx.openForRead(ref, LOCKMODE_COMMIT);

        ref.___abort(tx, tranlocal, pool);

        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
        assertEquals(version, ref.getVersion());
        assertEquals(value, ref.___weakRead());

    }

    @Test
    public void whenEnsuredByOtherAndOpenedForRead() {
        BetaLongRef ref = newLongRef(stm);
        long version = ref.getVersion();
        long value = ref.___weakRead();


        BetaTransaction tx = stm.startDefaultTransaction();
        LongRefTranlocal read2 = tx.openForRead(ref, LOCKMODE_NONE);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.ensure(otherTx);

        ref.___abort(tx, read2, pool);

        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
        assertEquals(version, ref.getVersion());
        assertEquals(value, ref.___weakRead());
    }

    @Test
    public void whenPrivatizedByOtherAndOpenedForRead() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        LongRefTranlocal read2 = tx.openForRead(ref, LOCKMODE_NONE);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.privatize(otherTx);

        ref.___abort(tx, read2, pool);

        assertHasNoUpdateLock(ref);
        assertHasCommitLock(ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void whenPrivatizedBySelfAndOpenedForWrite() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_COMMIT);
        ref.___abort(tx, write, pool);

        assertHasNoUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(0, ref);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void whenEnsuredBySelfAndOpenedForWrite() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_UPDATE);

        ref.___abort(tx, write, pool);

        assertHasNoUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(0, ref);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void whenLockedByOtherAndOpenedForWrite() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.privatize(otherTx);

        ref.___abort(tx, write, pool);

        assertHasCommitLock(ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void whenListenersAvailable_theyRemain() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.get(otherTx);
        Latch listener = mock(Latch.class);
        otherTx.registerChangeListenerAndAbort(listener);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.get(tx);
        tx.abort();

        assertIsAborted(tx);
        assertHasListeners(ref, listener);
    }
}
