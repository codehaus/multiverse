package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.blocking.CheapLatch;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.Listeners;
import org.multiverse.stms.beta.conflictcounters.GlobalConflictCounter;
import org.multiverse.stms.beta.orec.Orec;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.createReadBiasedLongRef;
import static org.multiverse.TestUtils.getField;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class BetaLongRef_commitDirtyTest implements BetaStmConstants {

    private BetaStm stm;
    private BetaObjectPool pool;
    private GlobalConflictCounter globalConflictCounter;

    @Before
    public void setUp() {
        stm = new BetaStm();
        globalConflictCounter = stm.getGlobalConflictCounter();
        pool = new BetaObjectPool();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenWriteHasListeners() {
        BetaLongRef ref = newLongRef(stm);
        Orec orec = ref.___getOrec();

        CheapLatch latch = new CheapLatch();
        long listenerEra = latch.getEra();
        ref.___registerChangeListener(latch, ref.___unsafeLoad(), pool, listenerEra);

        BetaTransaction tx = stm.startDefaultTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        write.value++;
        write.isDirty = DIRTY_TRUE;
        ref.___tryLockAndCheckConflict(tx, 1, write, true);

        long oldConflictCount = globalConflictCounter.count();
        Listeners listeners = ref.___commitDirty(write, tx, pool);

        assertNotNull(listeners);
        assertNull(listeners.next);
        assertEquals(listenerEra, listeners.listenerEra);
        assertSame(latch, listeners.listener);
        assertNull(getField(ref, "___listeners"));
        assertEquals(oldConflictCount, globalConflictCounter.count());
        assertSame(write, ref.___unsafeLoad());
        assertNull(write.read);
        assertSame(ref, write.owner);
        assertHasNoCommitLock(orec);
        assertSurplus(0, orec);
        assertReadonlyCount(0, orec);
        assertFalse(latch.isOpen());
        assertEquals(listenerEra, latch.getEra());
    }

    @Test
    public void whenPrivatizedBySelfAndOnlyRead() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.privatize(tx);
        ref.get(tx);
        LongRefTranlocal tranlocal = (LongRefTranlocal) tx.get(ref);
        ref.___commitDirty(tranlocal, tx, pool);

        assertHasNoUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertSurplus(0, ref);
        assertReadonlyCount(1, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenPrivatizedBySelfAndDirtyWrite() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.privatize(tx);
        ref.set(tx, 1);
        LongRefTranlocal tranlocal = (LongRefTranlocal) tx.get(ref);
        tranlocal.isDirty = DIRTY_TRUE;
        ref.___commitDirty(tranlocal, tx, pool);

        assertHasNoUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSame(tranlocal, ref.___unsafeLoad());
        assertSurplus(0, ref);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
        assertTrue(tranlocal.isCommitted);
    }

    @Test
    public void whenPrivatizedBySelfAndNoDirtyWrite() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.privatize(tx);
        ref.set(tx, 0);
        LongRefTranlocal tranlocal = (LongRefTranlocal) tx.get(ref);
        tranlocal.isDirty = DIRTY_FALSE;
        ref.___commitDirty(tranlocal, tx, pool);

        assertHasNoUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertSurplus(0, ref);
        assertReadonlyCount(1, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenEnsuredBySelfAndOnlyRead() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.ensure(tx);
        ref.get(tx);
        LongRefTranlocal tranlocal = (LongRefTranlocal) tx.get(ref);
        ref.___commitDirty(tranlocal, tx, pool);

        assertHasNoUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertSurplus(0, ref);
        assertReadonlyCount(1, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenEnsuredBySelfAndDirtyWrite() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.ensure(tx);
        ref.set(tx, 1);
        LongRefTranlocal tranlocal = (LongRefTranlocal) tx.get(ref);
        tranlocal.isDirty = DIRTY_TRUE;
        ref.___commitDirty(tranlocal, tx, pool);

        assertHasNoUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSame(tranlocal, ref.___unsafeLoad());
        assertSurplus(0, ref);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
        assertTrue(tranlocal.isCommitted);
    }

    @Test
    public void whenEnsuredBySelfAndNoDirtyWrite() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.ensure(tx);
        ref.set(tx, 0);
        LongRefTranlocal tranlocal = (LongRefTranlocal) tx.get(ref);
        tranlocal.isDirty = DIRTY_FALSE;
        ref.___commitDirty(tranlocal, tx, pool);

        assertHasNoUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertSurplus(0, ref);
        assertReadonlyCount(1, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenCommitWithConflict() {
        BetaLongRef ref = newLongRef(stm);
        Orec orec = ref.___getOrec();

        BetaTransaction tx = stm.startDefaultTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        write.isDirty = DIRTY_TRUE;
        write.value++;

        BetaTransaction otherTx = stm.startDefaultTransaction();
        Tranlocal conflictingRead = otherTx.openForRead(ref, LOCKMODE_NONE);

        long oldConflictCount = globalConflictCounter.count();
        ref.___tryLockAndCheckConflict(tx, 1, write, true);
        Listeners listeners = ref.___commitDirty(write, tx, pool);

        assertNull(listeners);
        assertEquals(oldConflictCount + 1, globalConflictCounter.count());
        assertSame(write, ref.___unsafeLoad());
        assertNull(write.read);
        assertSame(ref, write.owner);
        assertHasNoCommitLock(orec);
        assertSurplus(1, orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenDirtyWrite() {
        BetaLongRef ref = newLongRef(stm);
        Orec orec = ref.___getOrec();

        BetaTransaction tx = stm.startDefaultTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        write.value++;
        write.isDirty = DIRTY_TRUE;
        ref.___tryLockAndCheckConflict(tx, 1, write, true);

        long oldConflictCount = globalConflictCounter.count();
        Listeners listeners = ref.___commitDirty(write, tx, pool);

        assertNull(listeners);
        assertEquals(oldConflictCount, globalConflictCounter.count());
        assertSame(write, ref.___unsafeLoad());
        assertNull(write.read);
        assertSame(ref, write.owner);
        assertHasNoCommitLock(orec);
        assertSurplus(0, orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenNonDirtyWrite() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();
        Orec orec = ref.___getOrec();

        BetaTransaction tx = stm.startDefaultTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        write.isDirty = DIRTY_FALSE;
        ref.___tryLockAndCheckConflict(tx, 1, write, true);

        long oldConflictCount = globalConflictCounter.count();
        Listeners listeners = ref.___commitDirty(write, tx, pool);

        assertNull(listeners);
        assertEquals(oldConflictCount, globalConflictCounter.count());
        assertSame(committed, ref.___unsafeLoad());
        assertFalse(write.isCommitted);
        assertHasNoCommitLock(orec);
        assertSurplus(0, orec);
        assertReadonlyCount(1, orec);
    }

    @Test
    public void whenCommitOnReadBiasedOrec() {
        BetaLongRef ref = createReadBiasedLongRef(stm);

        Orec orec = ref.___getOrec();
        assertHasNoCommitLock(orec);

        BetaTransaction tx = stm.startDefaultTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        write.value++;
        write.isDirty = DIRTY_TRUE;
        ref.___tryLockAndCheckConflict(tx, 1, write, true);

        long oldConflictCount = globalConflictCounter.count();
        Listeners listeners = ref.___commitDirty(write, tx, pool);

        assertNull(listeners);
        assertEquals(oldConflictCount + 1, globalConflictCounter.count());
        assertSame(write, ref.___unsafeLoad());
        assertNull(write.read);
        assertSame(ref, write.owner);
        assertHasNoCommitLock(orec);
        assertUpdateBiased(orec);
        assertSurplus(0, orec);
        assertReadonlyCount(0, orec);
    }
}
