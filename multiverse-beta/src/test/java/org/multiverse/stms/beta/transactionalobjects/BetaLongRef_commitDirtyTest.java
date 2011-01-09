package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.LockMode;
import org.multiverse.api.blocking.DefaultRetryLatch;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.Listeners;
import org.multiverse.stms.beta.conflictcounters.GlobalConflictCounter;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.getField;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmTestUtils.*;
import static org.multiverse.stms.beta.transactionalobjects.OrecTestUtils.*;

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
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        DefaultRetryLatch latch = new DefaultRetryLatch();
        long listenerEra = latch.getEra();
        BetaLongRefTranlocal load = ref.___newTranlocal();
        ref.___load(1, null, LOCKMODE_NONE, load);
        ref.___registerChangeListener(latch, load, pool, listenerEra);
        ref.___abort(null, load, pool);

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRefTranlocal tranlocal = tx.openForWrite(ref, LOCKMODE_NONE);
        tranlocal.value++;
        tranlocal.setDirty(true);
        tranlocal.prepareDirtyUpdates(pool, tx, 1);
        long oldConflictCount = globalConflictCounter.count();
        Listeners listeners = ref.___commitDirty(tranlocal, tx, pool);

        assertNotNull(listeners);
        assertNull(listeners.next);
        assertEquals(listenerEra, listeners.listenerEra);
        assertSame(latch, listeners.listener);
        assertNull(getField(ref, "___listeners"));
        assertEquals(oldConflictCount, globalConflictCounter.count());
        assertRefHasNoLocks(ref);
        assertSurplus(0, ref);
        assertReadonlyCount(0, ref);
        assertFalse(latch.isOpen());
        assertEquals(listenerEra, latch.getEra());
        assertVersionAndValue(ref, initialVersion + 1, initialValue + 1);
    }

    @Test
    public void whenCommittingDirtyUpdateWithConflictingListener_thenGlobalConflictCounterIncreased() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRefTranlocal tranlocal = tx.openForWrite(ref, LOCKMODE_NONE);
        tranlocal.value++;

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_NONE);

        long oldConflictCount = globalConflictCounter.count();

        tranlocal.prepareDirtyUpdates(pool, tx, 1);
        Listeners listeners = ref.___commitDirty(tranlocal, tx, pool);

        assertNull(listeners);
        assertEquals(oldConflictCount + 1, globalConflictCounter.count());
        assertRefHasNoLocks(ref);
        assertSurplus(1, ref);
        assertReadonlyCount(0, ref);
        assertVersionAndValue(ref, initialVersion + 1, initialValue + 1);
    }

    @Test
    public void whenCommittingNonDirtyUpdateWithConflictingListener_thenGlobalConflictCounterNotIncreased() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRefTranlocal tranlocal = tx.openForWrite(ref, LOCKMODE_NONE);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_NONE);

        long oldConflictCount = globalConflictCounter.count();

        tranlocal.prepareDirtyUpdates(pool, tx, 1);
        Listeners listeners = ref.___commitDirty(tranlocal, tx, pool);

        assertNull(listeners);
        assertEquals(oldConflictCount, globalConflictCounter.count());
        assertRefHasNoLocks(ref);
        assertSurplus(1, ref);
        assertReadonlyCount(1, ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenCommittingReadonlyWithConflictingListener_thenGlobalConflictCounterNotIncreased() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaTranlocal tranlocal = tx.openForRead(ref, LOCKMODE_NONE);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_NONE);

        tranlocal.prepareDirtyUpdates(pool, tx, 1);
        long oldConflictCount = globalConflictCounter.count();
        Listeners listeners = ref.___commitDirty(tranlocal, tx, pool);

        assertNull(listeners);
        assertEquals(oldConflictCount, globalConflictCounter.count());
        assertRefHasNoLocks(ref);
        assertSurplus(1, ref);
        assertReadonlyCount(1, ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenUpdateBiasedRead() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRefTranlocal tranlocal = tx.openForRead(ref, LOCKMODE_NONE);

        tranlocal.prepareDirtyUpdates(pool, tx, 1);
        long oldConflictCount = globalConflictCounter.count();
        Listeners result = ref.___commitDirty(tranlocal, tx, pool);

        assertNull(result);
        assertEquals(oldConflictCount, globalConflictCounter.count());
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasNoLocks(ref);
        assertSurplus(0, ref);
        assertReadonlyCount(1, ref);
    }

    @Test
    public void whenReadBiasedRead() {
        long initialValue = 10;
        BetaLongRef ref = makeReadBiased(newLongRef(stm, initialValue));
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRefTranlocal tranlocal = tx.openForRead(ref, LOCKMODE_NONE);

        tranlocal.prepareDirtyUpdates(pool, tx, 1);
        long oldConflictCount = globalConflictCounter.count();
        Listeners result = ref.___commitDirty(tranlocal, tx, pool);

        assertNull(result);
        assertEquals(oldConflictCount, globalConflictCounter.count());
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasNoLocks(ref);
        assertSurplus(1, ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenDirtyWrite_thenChangeWritten() {
        long initialValue = 10;
        long updateValue = 20;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRefTranlocal tranlocal = tx.openForWrite(ref, LOCKMODE_NONE);
        tranlocal.value = updateValue;

        tranlocal.prepareDirtyUpdates(pool, tx, 1);
        long oldConflictCount = globalConflictCounter.count();
        Listeners result = ref.___commitDirty(tranlocal, tx, pool);

        assertNull(result);
        assertEquals(oldConflictCount, globalConflictCounter.count());
        assertVersionAndValue(ref, initialVersion + 1, updateValue);

        assertRefHasNoLocks(ref);
        assertSurplus(0, ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenNonDirtyWrite_thenNoChangeWritten() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaTranlocal tranlocal = tx.openForWrite(ref, LOCKMODE_NONE);
        tranlocal.prepareDirtyUpdates(pool, tx, 1);

        long oldConflictCount = globalConflictCounter.count();
        Listeners result = ref.___commitDirty(tranlocal, tx, pool);

        assertNull(result);
        assertEquals(oldConflictCount, globalConflictCounter.count());
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasNoLocks(ref);
        assertSurplus(0, ref);
        assertReadonlyCount(1, ref);
    }

    @Test
    public void whenCommitOnReadBiasedRead_thenGlobalConflictCounterNotIncreased() {
        long initialValue = 10;
        BetaLongRef ref = makeReadBiased(newLongRef(stm, initialValue));
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaTranlocal tranlocal = tx.openForWrite(ref, LOCKMODE_NONE);
        tranlocal.prepareDirtyUpdates(pool, tx, 1);

        long oldConflictCount = globalConflictCounter.count();
        Listeners listeners = ref.___commitDirty(tranlocal, tx, pool);

        assertNull(listeners);
        assertEquals(oldConflictCount, globalConflictCounter.count());
        assertRefHasNoLocks(ref);
        assertReadBiased(ref);
        assertSurplus(1, ref);
        assertVersionAndValue(ref, version, initialValue);
    }

    @Test
    public void locked_whenPrivatizedBySelfAndOnlyRead() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Commit);
        ref.get(tx);
        BetaLongRefTranlocal tranlocal = (BetaLongRefTranlocal) tx.get(ref);
        tranlocal.prepareDirtyUpdates(pool, tx, 1);
        Listeners listeners = ref.___commitDirty(tranlocal, tx, pool);

        assertNull(listeners);
        assertRefHasNoLocks(ref);
        assertSurplus(0, ref);
        assertReadonlyCount(1, ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void locked_whenPrivatizedBySelfAndDirtyWrite() {
        long initialValue = 10;
        long updateValue = 20;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Commit);
        BetaLongRefTranlocal tranlocal = tx.openForWrite(ref, LOCKMODE_NONE);
        tranlocal.value = updateValue;
        tranlocal.prepareDirtyUpdates(pool, tx, 1);
        Listeners listeners = ref.___commitDirty(tranlocal, tx, pool);

        assertNull(listeners);
        assertRefHasNoLocks(ref);
        assertSurplus(0, ref);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, initialVersion + 1, updateValue);
    }

    @Test
    public void locked_whenPrivatizedBySelfAndNoDirtyWrite_thenLockReleasedAndNoChangeWritten() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Commit);
        BetaLongRefTranlocal tranlocal = tx.openForWrite(ref, LOCKMODE_NONE);
        tranlocal.value = initialValue;
        tranlocal.prepareDirtyUpdates(pool, tx, 1);
        Listeners listeners = ref.___commitDirty(tranlocal, tx, pool);

        assertNull(listeners);
        assertRefHasNoLocks(ref);
        assertSurplus(0, ref);
        assertReadonlyCount(1, ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void locked_whenEnsuredBySelfAndOnlyRead_thenLockReleasedAndNoChangeWritten() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Write);
        ref.get(tx);
        BetaLongRefTranlocal tranlocal = (BetaLongRefTranlocal) tx.get(ref);
        tranlocal.prepareDirtyUpdates(pool, tx, 1);
        Listeners listeners = ref.___commitDirty(tranlocal, tx, pool);

        assertNull(listeners);
        assertRefHasNoLocks(ref);
        assertSurplus(0, ref);
        assertReadonlyCount(1, ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void locked_whenEnsuredBySelfAndDirtyWrite_thenLockReleasedAndChangeWritten() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Write);
        int updatedValue = 1;
        ref.set(tx, updatedValue);
        BetaLongRefTranlocal tranlocal = (BetaLongRefTranlocal) tx.get(ref);
        tranlocal.prepareDirtyUpdates(pool, tx, 1);
        Listeners listeners = ref.___commitDirty(tranlocal, tx, pool);

        assertNull(listeners);
        assertRefHasNoLocks(ref);
        assertSurplus(0, ref);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, initialVersion + 1, updatedValue);
    }

    @Test
    public void locked_whenEnsuredBySelfAndNoDirtyWrite_thenLockReleasedAndNoChangeWritten() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Write);
        ref.set(tx, initialValue);
        BetaLongRefTranlocal tranlocal = (BetaLongRefTranlocal) tx.get(ref);
        tranlocal.prepareDirtyUpdates(pool, tx, 1);
        Listeners listeners = ref.___commitDirty(tranlocal, tx, pool);

        assertNull(listeners);
        assertRefHasNoLocks(ref);
        assertSurplus(0, ref);
        assertReadonlyCount(1, ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenCommittingDirtyConstructedObject() {
        long initialValue = 10;

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        BetaLongRefTranlocal tranlocal = tx.openForConstruction(ref);
        tranlocal.value = 10;

        tranlocal.prepareDirtyUpdates(pool, tx, 1);

        long oldConflictCount = globalConflictCounter.count();
        Listeners result = ref.___commitDirty(tranlocal, tx, pool);

        assertNull(result);
        assertEquals(oldConflictCount, globalConflictCounter.count());
        assertVersionAndValue(ref, BetaLongRef.VERSION_UNCOMMITTED + 1, initialValue);
        assertRefHasNoLocks(ref);
        assertSurplus(0, ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenCommittingNonDirtyConstructedObject() {
        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        BetaLongRefTranlocal tranlocal = tx.openForConstruction(ref);

        tranlocal.prepareDirtyUpdates(pool, tx, 1);

        long oldConflictCount = globalConflictCounter.count();
        Listeners result = ref.___commitDirty(tranlocal, tx, pool);

        assertNull(result);
        assertEquals(oldConflictCount, globalConflictCounter.count());
        assertVersionAndValue(ref, BetaLongRef.VERSION_UNCOMMITTED + 1, 0);
        assertRefHasNoLocks(ref);
        assertSurplus(0, ref);
        assertReadonlyCount(0, ref);
    }
}
