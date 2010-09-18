package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
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
        LongRefTranlocal write = tx.openForWrite(ref, false);
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
    @Ignore
    public void whenPrivatizedBySelfAndOnlyRead() {

    }

    @Test
    @Ignore
    public void whenPrivatizedBySelfAndDirtyWrite() {

    }

    @Test
    @Ignore
    public void whenPrivatizedBySelfAndNoDirtyWrite() {

    }

    @Test
    @Ignore
    public void whenEnsuredBySelfAndOnlyRead() {

    }

    @Test
    @Ignore
    public void whenEnsuredBySelfAndDirtyWrite() {

    }

    @Test
    @Ignore
    public void whenEnsuredBySelfAndNoDirtyWrite() {

    }

    @Test
    public void whenCommitWithConflict() {
        BetaLongRef ref = newLongRef(stm);
        Orec orec = ref.___getOrec();

        BetaTransaction tx = stm.startDefaultTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, false);
        write.isDirty = DIRTY_TRUE;
        write.value++;

        BetaTransaction otherTx = stm.startDefaultTransaction();
        Tranlocal conflictingRead = otherTx.openForRead(ref, false);

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
        LongRefTranlocal write = tx.openForWrite(ref, false);
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
        LongRefTranlocal write = tx.openForWrite(ref, false);
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
        LongRefTranlocal write = tx.openForWrite(ref, false);
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
