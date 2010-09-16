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
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class BetaLongRef_commitAllTest implements BetaStmConstants {
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
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);
        Orec orec = ref.___getOrec();

        CheapLatch latch = new CheapLatch();
        long listenerEra = latch.getEra();
        ref.___registerChangeListener(latch, ref.___unsafeLoad(), pool, listenerEra);

        BetaTransaction tx = stm.startDefaultTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, false);
        ref.___tryLockAndCheckConflict(tx, 1, write);

        long oldConflictCount = globalConflictCounter.count();
        Listeners listeners = ref.___commitAll(write, tx, pool);

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
    public void whenCommitWithConflict() {
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);
        Orec orec = ref.___getOrec();

        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal tranlocal = tx.openForWrite(ref, false);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        Tranlocal read2 = otherTx.openForRead(ref, false);

        ref.___tryLockAndCheckConflict(tx, 1, read2);

        long oldConflictCount = globalConflictCounter.count();
        Listeners listeners = ref.___commitAll(tranlocal, tx, pool);

        assertNull(listeners);
        assertEquals(oldConflictCount + 1, globalConflictCounter.count());
        assertSame(tranlocal, ref.___unsafeLoad());
        assertNull(tranlocal.read);
        assertSame(ref, tranlocal.owner);
        assertHasNoCommitLock(orec);
        assertSurplus(1, orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenDirtyWrite() {
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);
        Orec orec = ref.___getOrec();

        BetaTransaction tx = stm.startDefaultTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, false);
        write.value++;
        write.isDirty = DIRTY_TRUE;
        ref.___tryLockAndCheckConflict(tx, 1, write);

        long oldConflictCount = globalConflictCounter.count();
        Listeners result = ref.___commitAll(write, tx, pool);

        assertNull(result);
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
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);
        Orec orec = ref.___getOrec();

        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal write = tx.openForWrite(ref, false);
        ref.___tryLockAndCheckConflict(tx, 1, write);

        long oldConflictCount = globalConflictCounter.count();
        Listeners result = ref.___commitAll(write, tx, pool);

        assertNull(result);
        assertEquals(oldConflictCount, globalConflictCounter.count());
        assertSame(write, ref.___unsafeLoad());
        assertNull(write.read);
        assertSame(ref, write.owner);
        assertHasNoCommitLock(orec);
        assertSurplus(0, orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenCommitOnReadBiasedOrec() {
        BetaLongRef ref = createReadBiasedLongRef(stm);

        Orec orec = ref.___getOrec();
        assertHasNoCommitLock(orec);

        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal tranlocal = tx.openForWrite(ref, false);
        ref.___tryLockAndCheckConflict(tx, 1, tranlocal);

        long oldConflictCount = globalConflictCounter.count();
        Listeners listeners = ref.___commitAll(tranlocal, tx, pool);

        assertNull(listeners);
        assertEquals(oldConflictCount + 1, globalConflictCounter.count());
        assertSame(tranlocal, ref.___unsafeLoad());
        assertNull(tranlocal.read);
        assertSame(ref, tranlocal.owner);
        assertHasNoCommitLock(orec);
        assertUpdateBiased(orec);
        assertSurplus(0, orec);
        assertReadonlyCount(0, orec);
    }
}
