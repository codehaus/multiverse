package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadConflict;
import org.multiverse.api.functions.IncLongFunction;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmUtils;
import org.multiverse.stms.beta.transactionalobjects.LongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;
import org.multiverse.stms.beta.transactionalobjects.Tranlocal;

import static junit.framework.Assert.assertSame;
import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class FatArrayTreeBetaTransaction_openForReadTest {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void whenNull() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        Tranlocal result = tx.openForRead((LongRef) null, true, pool);

        assertNull(result);
        assertActive(tx);
    }

    @Test
    public void whenUntracked() {
        LongRef ref = createReadBiasedLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setBlockingAllowed(false)
                .setReadTrackingEnabled(false);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        assertActive(tx);
        assertSame(committed, read);
        assertNull(tx.get(ref));
        assertTrue((Boolean) getField(tx, "hasReads"));
        assertTrue((Boolean) getField(tx, "hasUntrackedReads"));
        assertSurplus(1, ref);
        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());
        assertNotAttached(tx, ref);
    }

    @Test
    public void whenUpdateBiased() {
        LongRef ref = createLongRef(stm, 10);
        Tranlocal committed = ref.___unsafeLoad();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        assertActive(tx);
        assertSame(committed, read);
        assertSurplus(1, ref.___getOrec());
        assertUnlocked(ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
        assertReadonlyCount(0, ref.___getOrec());
        assertSame(ref, read.owner);
        assertTrue(committed.isCommitted);
        assertFalse(committed.isPermanent);
        assertEquals(10, read.value);
        assertAttached(tx, read);
    }

    @Test
    public void whenReadBiased() {
        LongRef ref = createReadBiasedLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        assertActive(tx);
        assertSame(committed, read);
        assertSurplus(1, ref);
        assertUnlocked(ref);
        assertReadBiased(ref);
        assertReadonlyCount(0, ref);
        assertTrue(read.isCommitted);
        assertTrue(read.isPermanent);
        assertEquals(10, read.value);
        assertAttached(tx, read);
    }

    @Test
    public void whenReadBiasedAndNoReadTrackingAndLock_thenAttached() {
        LongRef ref = createReadBiasedLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setBlockingAllowed(false)
                .setReadTrackingEnabled(false);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);
        LongRefTranlocal read = tx.openForRead(ref, true, pool);

        assertActive(tx);
        assertSame(committed, read);
        assertSurplus(1, ref);
        assertLocked(ref);
        assertSame(tx, ref.___getLockOwner());
        assertReadBiased(ref);
        assertReadonlyCount(0, ref);
        assertTrue(read.isCommitted);
        assertTrue(read.isPermanent);
        assertEquals(10, read.value);
        assertAttached(tx, read);
    }


    @Test
    public void whenAlreadyOpenedForRead() {
        LongRef ref = createLongRef(stm, 10);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongRefTranlocal read1 = tx.openForRead(ref, false, pool);
        LongRefTranlocal read2 = tx.openForRead(ref, false, pool);

        assertActive(tx);
        assertSame(read1, read2);
        assertUnlocked(ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
        assertReadonlyCount(0, ref.___getOrec());
        assertSurplus(1, ref.___getOrec());
        assertAttached(tx, read2);
    }

    @Test
    public void whenAlreadyOpenedForWrite() {
        LongRef ref = createLongRef(stm, 10);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        assertActive(tx);
        assertSame(write, read);
        assertUnlocked(ref);
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertNull(ref.___getLockOwner());
        assertAttached(tx, read);
    }

    @Test
    public void whenLockedByOther_thenLockedConflict() {
        LongRef ref = createLongRef(stm);
        Tranlocal committed = ref.___unsafeLoad();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, true, pool);

        FatArrayTreeBetaTransaction tx2 = new FatArrayTreeBetaTransaction(stm);
        try {
            tx2.openForRead(ref, false, pool);
            fail();
        } catch (ReadConflict expected) {
        }

        assertAborted(tx2);
        assertSame(committed, ref.___unsafeLoad());
        assertLocked(ref.___getOrec());
        assertReadonlyCount(0, ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
        assertSurplus(1, ref.___getOrec());
        assertSame(otherTx, ref.___getLockOwner());
    }

    @Test
    public void whenLock() {
        LongRef ref = createLongRef(stm);
        Tranlocal committed = ref.___unsafeLoad();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, true, pool);

        assertActive(tx);
        assertSame(committed, read);
        assertLocked(ref);
        assertSame(tx, ref.___getLockOwner());
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertAttached(tx, read);
    }

    @Test
    public void whenAlreadyLockedBySelf_thenNoProblem() {
        LongRef ref = createLongRef(stm);
        Tranlocal committed = ref.___unsafeLoad();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongRefTranlocal read1 = tx.openForRead(ref, true, pool);
        LongRefTranlocal read2 = tx.openForRead(ref, true, pool);

        assertActive(tx);
        assertSame(read1, read2);
        assertSame(committed, read2);
        assertLocked(ref);
        assertSame(tx, ref.___getLockOwner());
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertAttached(tx, read2);
    }

    @Test
    public void whenPessimisticRead() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.Read);
        BetaTransaction tx = new FatArrayTreeBetaTransaction(config);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        assertSame(committed, read);
        assertLocked(ref);
        assertSame(tx, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertTrue(read.isCommitted);
        assertFalse(read.isPermanent);
        assertAttached(tx, read);
    }

    @Test
    public void whenReadConflict() {
        LongRef ref1 = createLongRef(stm);
        LongRef ref2 = createLongRef(stm);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongRefTranlocal read1 = tx.openForRead(ref1, false, pool);

        FatArrayTreeBetaTransaction conflictingTx = new FatArrayTreeBetaTransaction(stm);
        conflictingTx.openForWrite(ref1, false, pool).value++;
        conflictingTx.openForWrite(ref2, false, pool).value++;
        conflictingTx.commit(pool);

        try {
            tx.openForRead(ref2, false, pool);
            fail();
        } catch (ReadConflict expected) {
        }

        assertAborted(tx);

        assertUnlocked(ref1);
        assertNull(ref1.___getLockOwner());
        assertSurplus(0, ref1);
        assertUpdateBiased(ref1);

        assertUnlocked(ref2);
        assertNull(ref2.___getLockOwner());
        assertSurplus(0, ref2);
        assertUpdateBiased(ref2);
    }

    @Test
    public void whenAlreadyOpenedForConstruction() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongRef ref = new LongRef(tx);
        LongRefTranlocal construction = tx.openForConstruction(ref, pool);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        assertActive(tx);
        assertSame(construction, read);
        assertLocked(ref);
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertSame(tx, ref.___getLockOwner());
        assertNull(ref.___unsafeLoad());
        assertAttached(tx, read);
    }

    @Test
    public void whenConstructedAndLock() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongRef ref = new LongRef(tx);
        LongRefTranlocal construction = tx.openForConstruction(ref, pool);

        LongRefTranlocal read = tx.openForRead(ref, true, pool);

        assertActive(tx);
        assertSame(construction, read);
        assertNull(ref.___unsafeLoad());
        assertLocked(ref);
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertSame(tx, ref.___getLockOwner());
        assertAttached(tx, read);
    }

    @Test
    public void conflictCounterIsOnlySetOnFirstRead() {
        LongRef ref1 = createLongRef(stm);
        LongRef ref2 = createLongRef(stm);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        stm.getGlobalConflictCounter().signalConflict(ref1);

        LongRefTranlocal read1 = tx.openForRead(ref1, false, pool);

        assertEquals(tx.getLocalConflictCounter().get(), stm.getGlobalConflictCounter().count());

        LongRefTranlocal read2 = tx.openForRead(ref2, false, pool);

        assertEquals(tx.getLocalConflictCounter().get(), stm.getGlobalConflictCounter().count());
        assertActive(tx);
        assertAttached(tx, read1);
        assertAttached(tx, read2);
    }

    @Test
    public void whenContainsUntrackedRead_thenCantRecoverFromUnrealReadConflict() {
        LongRef ref1 = createReadBiasedLongRef(stm, 100);
        LongRef ref2 = createLongRef(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setReadTrackingEnabled(false);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);
        tx.openForRead(ref1, false, pool);

        //an unreal readconflict
        stm.getGlobalConflictCounter().signalConflict(createLongRef(stm));

        try {
            tx.openForRead(ref2, false, pool);
            fail();
        } catch (ReadConflict expected) {
        }

        assertAborted(tx);
        assertSurplus(1, ref1);
        assertUnlocked(ref1);
        assertNull(ref1.___getLockOwner());
        assertSurplus(0, ref2);
        assertUnlocked(ref2);
        assertNull(ref2.___getLockOwner());
    }

    @Test
    public void whenNonConflictReadConflict() {
        LongRef ref1 = createLongRef(stm);
        LongRef ref2 = createLongRef(stm);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongRefTranlocal read1 = tx.openForRead(ref1, false, pool);
        long oldLocalConflictCounter = tx.getLocalConflictCounter().get();

        stm.getGlobalConflictCounter().signalConflict(createLongRef(stm));
        LongRefTranlocal read2 = tx.openForRead(ref2, false, pool);

        assertFalse(oldLocalConflictCounter == stm.getGlobalConflictCounter().count());
        assertEquals(tx.getLocalConflictCounter().get(), stm.getGlobalConflictCounter().count());
        assertActive(tx);
        assertAttached(tx, read1);
        assertAttached(tx, read2);
    }

    @Test
    public void whenPessimisticThenNoConflictDetectionNeeded() {
        LongRef ref1 = createLongRef(stm);
        LongRef ref2 = createLongRef(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.Read);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);
        LongRefTranlocal read1 = tx.openForRead(ref1, false, pool);

        long oldLocalConflictCount = tx.getLocalConflictCounter().get();

        stm.getGlobalConflictCounter().signalConflict(createLongRef(stm));

        LongRefTranlocal read2 = tx.openForRead(ref2, false, pool);

        assertEquals(oldLocalConflictCount, tx.getLocalConflictCounter().get());
        assertActive(tx);
        assertAttached(tx, read1);
        assertAttached(tx, read2);
    }

    @Test
    public void whenHasCommutingFunctions() {
        LongRef ref = createLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongFunction function = new IncLongFunction();
        tx.commute(ref, pool, function);

        LongRefTranlocal commuting = (LongRefTranlocal) tx.get(ref);

        LongRefTranlocal read = tx.openForWrite(ref, false, pool);

        assertActive(tx);
        assertSame(commuting, read);
        assertSame(committed, read.read);
        assertFalse(read.isCommuting);
        assertFalse(read.isCommitted);
        assertEquals(11, read.value);
        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertAttached(tx, read);
    }

    @Test
    public void whenHasCommutingFunctionsAndLockedByOther_thenReadConflict() {
        LongRef ref = createLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction otherTx = new FatArrayBetaTransaction(stm);
        otherTx.openForRead(ref, true, pool);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongFunction function = new IncLongFunction();
        tx.commute(ref, pool, function);

        try {
            tx.openForRead(ref, false, pool);
            fail();
        } catch (ReadConflict expected) {
        }

        assertAborted(tx);
        assertSame(otherTx, ref.___getLockOwner());
        assertLocked(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void conflictCounterIsSetAtFirstRead() {
        LongRef ref = createLongRef(stm, 10);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);

        stm.getGlobalConflictCounter().signalConflict(ref);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        assertEquals(stm.getGlobalConflictCounter().count(), tx.getLocalConflictCounter().get());
        assertActive(tx);
        assertAttached(tx, read);
    }

    @Test
    public void conflictCounterIsNotSetWhenAlreadyRead() {
        LongRef ref = createLongRef(stm, 10);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);

        stm.getGlobalConflictCounter().signalConflict(createLongRef(stm));

        LongRefTranlocal read1 = tx.openForRead(ref, false, pool);
        long localConflictCount = tx.getLocalConflictCounter().get();
        stm.getGlobalConflictCounter().signalConflict(createLongRef(stm));

        LongRefTranlocal read2 = tx.openForRead(ref, false, pool);

        assertEquals(localConflictCount, tx.getLocalConflictCounter().get());
        assertActive(tx);
        assertSame(read1, read2);
        assertAttached(tx, read1);
    }

    @Test
    public void whenUnrealConflictThenConflictCounterUpdated() {
        LongRef ref1 = createLongRef(stm);
        LongRef ref2 = createLongRef(stm);
        LongRef ref3 = createLongRef(stm);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);

        stm.getGlobalConflictCounter().signalConflict(createLongRef(stm));

        LongRefTranlocal read1 = tx.openForRead(ref1, false, pool);

        //do second read
        stm.getGlobalConflictCounter().signalConflict(createLongRef(stm));
        LongRefTranlocal read2 = tx.openForRead(ref2, false, pool);
        assertEquals(stm.getGlobalConflictCounter().count(), tx.getLocalConflictCounter().get());

        //do another read
        stm.getGlobalConflictCounter().signalConflict(createLongRef(stm));
        LongRefTranlocal read3 = tx.openForRead(ref3, false, pool);
        assertEquals(stm.getGlobalConflictCounter().count(), tx.getLocalConflictCounter().get());

        assertActive(tx);
        assertAttached(tx, read1);
        assertAttached(tx, read2);
        assertAttached(tx, read3);
    }

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.prepare(pool);

        LongRef ref = BetaStmUtils.createLongRef(stm);

        try {
            tx.openForRead(ref, true, pool);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.abort(pool);

        LongRef ref = BetaStmUtils.createLongRef(stm);

        try {
            tx.openForRead(ref, true, pool);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.commit(pool);

        LongRef ref = BetaStmUtils.createLongRef(stm);

        try {
            tx.openForRead(ref, true, pool);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertCommitted(tx);
    }
}
