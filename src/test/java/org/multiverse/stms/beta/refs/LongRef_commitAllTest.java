package org.multiverse.stms.beta.refs;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.blocking.CheapLatch;
import org.multiverse.api.blocking.Listeners;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.ObjectPool;
import org.multiverse.stms.beta.StmUtils;
import org.multiverse.stms.beta.conflictcounters.GlobalConflictCounter;
import org.multiverse.stms.beta.orec.Orec;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.createReadBiasedLongRef;
import static org.multiverse.TestUtils.getField;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class LongRef_commitAllTest {
    private BetaStm stm;
    private ObjectPool pool;
    private GlobalConflictCounter globalConflictCounter;

    @Before
    public void setUp() {
        stm = new BetaStm();
        globalConflictCounter = stm.getGlobalConflictCounter();
        pool = new ObjectPool();
    }

    @Test
    public void whenWriteHasListeners() {
        LongRef ref = StmUtils.createLongRef(stm);
        Orec orec = ref.getOrec();

        CheapLatch latch = new CheapLatch();
        long listenerEra = latch.getEra();
        ref.registerChangeListener(latch, ref.unsafeLoad(), pool, listenerEra);

        BetaTransaction tx = stm.start();
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        ref.tryLockAndCheckConflict(tx, 1, write);

        long oldConflictCount = globalConflictCounter.count();
        Listeners listeners = ref.commitAll(write, tx, pool, globalConflictCounter);

        assertNotNull(listeners);
        assertNull(listeners.next);
        assertEquals(listenerEra, listeners.listenerEra);
        assertSame(latch, listeners.listener);
        assertNull(getField(ref, "listeners"));
        assertEquals(oldConflictCount, globalConflictCounter.count());
        assertSame(write, ref.unsafeLoad());
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
        LongRef ref = StmUtils.createLongRef(stm);
        Orec orec = ref.getOrec();

        BetaTransaction tx = stm.start();
        Tranlocal tranlocal = tx.openForWrite(ref, false, pool);

        BetaTransaction otherTx = stm.start();
        Tranlocal read2 = otherTx.openForRead(ref, false, pool);

        ref.tryLockAndCheckConflict(tx, 1, read2);

        long oldConflictCount = globalConflictCounter.count();
        Listeners listeners = ref.commitAll(tranlocal, tx, pool, globalConflictCounter);

        assertNull(listeners);
        assertEquals(oldConflictCount + 1, globalConflictCounter.count());
        assertSame(tranlocal, ref.unsafeLoad());
        assertNull(tranlocal.read);
        assertSame(ref, tranlocal.owner);
        assertUnlocked(orec);
        assertSurplus(1, orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenDirtyWrite() {
        LongRef ref = StmUtils.createLongRef(stm);
        Orec orec = ref.getOrec();

        BetaTransaction tx = stm.start();
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        write.value++;
        write.isDirty = true;
        ref.tryLockAndCheckConflict(tx, 1, write);

        long oldConflictCount = globalConflictCounter.count();
        Listeners result = ref.commitAll(write, tx, pool, globalConflictCounter);

        assertNull(result);
        assertEquals(oldConflictCount, globalConflictCounter.count());
        assertSame(write, ref.unsafeLoad());
        assertNull(write.read);
        assertSame(ref, write.owner);
        assertUnlocked(orec);
        assertSurplus(0, orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenNonDirtyWrite() {
        LongRef ref = StmUtils.createLongRef(stm);
        Orec orec = ref.getOrec();

        BetaTransaction tx = stm.start();
        Tranlocal write = tx.openForWrite(ref, false, pool);
        ref.tryLockAndCheckConflict(tx, 1, write);

        long oldConflictCount = globalConflictCounter.count();
        Listeners result = ref.commitAll(write, tx, pool, globalConflictCounter);

        assertNull(result);
        assertEquals(oldConflictCount, globalConflictCounter.count());
        assertSame(write, ref.unsafeLoad());
        assertNull(write.read);
        assertSame(ref, write.owner);
        assertUnlocked(orec);
        assertSurplus(0, orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenCommitOnReadBiasedOrec() {
        LongRef ref = createReadBiasedLongRef(stm);

        Orec orec = ref.getOrec();
        assertUnlocked(orec);

        BetaTransaction tx = stm.start();
        Tranlocal tranlocal = tx.openForWrite(ref, false, pool);
        ref.tryLockAndCheckConflict(tx, 1, tranlocal);

        long oldConflictCount = globalConflictCounter.count();
        Listeners listeners = ref.commitAll(tranlocal, tx, pool, globalConflictCounter);

        assertNull(listeners);
        assertEquals(oldConflictCount + 1, globalConflictCounter.count());
        assertSame(tranlocal, ref.unsafeLoad());
        assertNull(tranlocal.read);
        assertSame(ref, tranlocal.owner);
        assertUnlocked(orec);
        assertUpdateBiased(orec);
        assertSurplus(0, orec);
        assertReadonlyCount(0, orec);
    }
}
