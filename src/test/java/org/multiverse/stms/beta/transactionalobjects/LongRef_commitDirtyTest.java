package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.blocking.CheapLatch;
import org.multiverse.stms.beta.*;
import org.multiverse.stms.beta.conflictcounters.GlobalConflictCounter;
import org.multiverse.stms.beta.orec.Orec;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.createReadBiasedLongRef;
import static org.multiverse.TestUtils.getField;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class LongRef_commitDirtyTest implements BetaStmConstants{

    private BetaStm stm;
    private BetaObjectPool pool;
    private GlobalConflictCounter globalConflictCounter;

    @Before
    public void setUp() {
        stm = new BetaStm();
        globalConflictCounter = stm.getGlobalConflictCounter();
        pool = new BetaObjectPool();
    }

    @Test
    public void whenWriteHasListeners() {
        LongRef ref = BetaStmUtils.createLongRef(stm);
        Orec orec = ref.___getOrec();

        CheapLatch latch = new CheapLatch();
        long listenerEra = latch.getEra();
        ref.___registerChangeListener(latch, ref.___unsafeLoad(), pool, listenerEra);

        BetaTransaction tx = stm.startDefaultTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        write.value++;
        write.isDirty = DIRTY_TRUE;
        ref.___tryLockAndCheckConflict(tx, 1, write);

        long oldConflictCount = globalConflictCounter.count();
        Listeners listeners = ref.___commitDirty(write, tx, pool, globalConflictCounter);

        assertNotNull(listeners);
        assertNull(listeners.next);
        assertEquals(listenerEra, listeners.listenerEra);
        assertSame(latch, listeners.listener);
        assertNull(getField(ref, "___listeners"));
        assertEquals(oldConflictCount, globalConflictCounter.count());
        assertSame(write, ref.___unsafeLoad());
        assertNull(write.read);
        assertSame(ref, write.owner);
        assertUnlocked(orec);
        assertSurplus(0, orec);
        assertReadonlyCount(0, orec);
        assertFalse(latch.isOpen());
        assertEquals(listenerEra, latch.getEra());
    }

    @Test
    public void whenCommitWithConflict() {
        LongRef ref = BetaStmUtils.createLongRef(stm);
        Orec orec = ref.___getOrec();

        BetaTransaction tx = stm.startDefaultTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        write.isDirty = DIRTY_TRUE;
        write.value++;

        BetaTransaction otherTx = stm.startDefaultTransaction();
        Tranlocal conflictingRead = otherTx.openForRead(ref, false, pool);

        long oldConflictCount = globalConflictCounter.count();
        ref.___tryLockAndCheckConflict(tx, 1, write);
        Listeners listeners = ref.___commitDirty(write, tx, pool, globalConflictCounter);

        assertNull(listeners);
        assertEquals(oldConflictCount + 1, globalConflictCounter.count());
        assertSame(write, ref.___unsafeLoad());
        assertNull(write.read);
        assertSame(ref, write.owner);
        assertUnlocked(orec);
        assertSurplus(1, orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenDirtyWrite() {
        LongRef ref = BetaStmUtils.createLongRef(stm);
        Orec orec = ref.___getOrec();

        BetaTransaction tx = stm.startDefaultTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        write.value++;
        write.isDirty = DIRTY_TRUE;
        ref.___tryLockAndCheckConflict(tx, 1, write);

        long oldConflictCount = globalConflictCounter.count();
        Listeners listeners = ref.___commitDirty(write, tx, pool, globalConflictCounter);

        assertNull(listeners);
        assertEquals(oldConflictCount, globalConflictCounter.count());
        assertSame(write, ref.___unsafeLoad());
        assertNull(write.read);
        assertSame(ref, write.owner);
        assertUnlocked(orec);
        assertSurplus(0, orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenNonDirtyWrite() {
        LongRef ref = BetaStmUtils.createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();
        Orec orec = ref.___getOrec();

        BetaTransaction tx = stm.startDefaultTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        write.isDirty = DIRTY_FALSE;
        ref.___tryLockAndCheckConflict(tx, 1, write);

        long oldConflictCount = globalConflictCounter.count();
        Listeners listeners = ref.___commitDirty(write, tx, pool, globalConflictCounter);

        assertNull(listeners);
        assertEquals(oldConflictCount, globalConflictCounter.count());
        assertSame(committed, ref.___unsafeLoad());
        assertFalse(write.isCommitted);
        assertUnlocked(orec);
        assertSurplus(0, orec);
        assertReadonlyCount(1, orec);
    }

    @Test
    public void whenCommitOnReadBiasedOrec() {
        LongRef ref = createReadBiasedLongRef(stm);

        Orec orec = ref.___getOrec();
        assertUnlocked(orec);

        BetaTransaction tx = stm.startDefaultTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        write.value++;
        write.isDirty = DIRTY_TRUE;
        ref.___tryLockAndCheckConflict(tx, 1, write);

        long oldConflictCount = globalConflictCounter.count();
        Listeners listeners = ref.___commitDirty(write, tx, pool, globalConflictCounter);

        assertNull(listeners);
        assertEquals(oldConflictCount + 1, globalConflictCounter.count());
        assertSame(write, ref.___unsafeLoad());
        assertNull(write.read);
        assertSame(ref, write.owner);
        assertUnlocked(orec);
        assertUpdateBiased(orec);
        assertSurplus(0, orec);
        assertReadonlyCount(0, orec);
    }
}
