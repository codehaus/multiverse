package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadConflict;
import org.multiverse.api.exceptions.SpeculativeConfigurationError;
import org.multiverse.functions.IncLongFunction;
import org.multiverse.functions.LongFunction;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmUtils;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.LongRefTranlocal;
import org.multiverse.stms.beta.refs.Tranlocal;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class FatArrayBetaTransaction_openForReadTest {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    @Ignore
    public void whenUnstarted() {

    }

    @Test
    public void whenNullRef() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        Tranlocal read = tx.openForRead((LongRef) null, false, pool);

        assertNull(read);
        assertActive(tx);
    }

    @Test
    public void conflictCounterIsOnlySetOnFirstRead() {
        LongRef ref1 = createLongRef(stm);
        LongRef ref2 = createLongRef(stm);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        stm.getGlobalConflictCounter().signalConflict(ref1);

        tx.openForRead(ref1, false, pool);

        assertEquals(tx.getLocalConflictCounter().get(), stm.getGlobalConflictCounter().count());

        tx.openForRead(ref2, false, pool);

        assertEquals(tx.getLocalConflictCounter().get(), stm.getGlobalConflictCounter().count());
    }

    @Test
    public void whenOverflowing() {
        LongRef ref1 = createLongRef(stm);
        LongRef ref2 = createLongRef(stm);
        LongRef ref3 = createLongRef(stm);
        LongRef ref4 = createLongRef(stm);

        BetaTransactionConfig config = new BetaTransactionConfig(stm, 3);
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
        tx.openForRead(ref1, false, pool);
        tx.openForRead(ref2, false, pool);
        tx.openForRead(ref3, false, pool);
        try {
            tx.openForRead(ref4, false, pool);
            fail();
        } catch (SpeculativeConfigurationError expected) {
        }

        assertAborted(tx);
        assertEquals(4, config.getSpeculativeConfig().getMinimalLength());
    }

    @Test
    public void whenUntracked() {
        LongRef ref = createReadBiasedLongRef(stm, 100);
        LongRefTranlocal committed = ref.unsafeLoad();

        BetaTransactionConfig config = new BetaTransactionConfig(stm)
                .setBlockingAllowed(false)
                .setReadTrackingEnabled(false);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
        LongRefTranlocal tranlocal = tx.openForRead(ref, false, pool);

        assertSame(committed, tranlocal);
        assertNull(tx.get(ref));
        assertTrue((Boolean) getField(tx, "hasReads"));
        assertTrue((Boolean) getField(tx, "hasUntrackedReads"));
    }

    @Test
    public void whenReadBiased() {
        LongRef ref = createReadBiasedLongRef(stm, 100);
        LongRefTranlocal committed = ref.unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        assertSame(committed, read);
        assertEquals(100, read.value);
        assertTrue(read.isPermanent);
        assertTrue(read.isCommitted);
        assertUnlocked(ref.getOrec());
        assertSurplus(1, ref.getOrec());
        assertReadBiased(ref.getOrec());
        assertNull(ref.getLockOwner());
        assertSame(committed, ref.unsafeLoad());
        assertActive(tx);
    }

    @Test
    public void whenUpdateBiased() {
        LongRef ref = createLongRef(stm, 100);
        LongRefTranlocal committed = ref.unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        assertSame(committed, read);
        assertEquals(100, read.value);
        assertFalse(read.isPermanent);
        assertTrue(read.isCommitted);
        assertUnlocked(ref);
        assertSurplus(1, ref.getOrec());
        assertUpdateBiased(ref.getOrec());
        assertSame(committed, ref.unsafeLoad());
        assertNull(ref.getLockOwner());
        assertActive(tx);
    }

    @Test
    public void whenReadBiasedAndLock() {
        LongRef ref = createReadBiasedLongRef(stm, 100);
        LongRefTranlocal committed = ref.unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, true, pool);

        assertSame(committed, read);
        assertEquals(100, read.value);
        assertTrue(read.isPermanent);
        assertTrue(read.isCommitted);
        assertLocked(ref.getOrec());
        assertSurplus(1, ref.getOrec());
        assertReadBiased(ref.getOrec());
        assertSame(tx, ref.getLockOwner());
        assertSame(committed, ref.unsafeLoad());
        assertActive(tx);
    }

    @Test
    public void whenUpdateBiasedAndLock() {
        LongRef ref = createLongRef(stm, 100);
        LongRefTranlocal committed = ref.unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, true, pool);

        assertSame(committed, read);
        assertEquals(100, read.value);
        assertFalse(read.isPermanent);
        assertTrue(read.isCommitted);
        assertLocked(ref.getOrec());
        assertSurplus(1, ref.getOrec());
        assertUpdateBiased(ref.getOrec());
        assertSame(tx, ref.getLockOwner());
        assertSame(committed, ref.unsafeLoad());
        assertActive(tx);
    }

    @Test
    public void whenAlreadyOpenedForWrite() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        write.value = 100;
        Tranlocal read = tx.openForRead(ref, false, pool);

        assertSame(write, read);
        assertEquals(100, write.value);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertUnlocked(ref.getOrec());
        assertSurplus(1, ref.getOrec());
        assertUpdateBiased(ref.getOrec());
        assertActive(tx);
        assertNull(ref.getLockOwner());
        assertSame(committed, ref.unsafeLoad());
    }

    @Test
    public void whenAlreadyOpenedForWrite_thenNoReadConflict() {
        LongRef ref = createLongRef(stm, 100);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);

        FatArrayBetaTransaction conflictingTx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal conflictingWrite = conflictingTx.openForWrite(ref, false, pool);
        conflictingWrite.value++;
        conflictingTx.commit(pool);

        Tranlocal read = tx.openForRead(ref, false, pool);

        assertSame(write, read);
        assertEquals(100, write.value);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertUnlocked(ref.getOrec());
        assertSurplus(1, ref.getOrec());
        assertUpdateBiased(ref.getOrec());
        assertActive(tx);
        assertNull(ref.getLockOwner());
        assertSame(conflictingWrite, ref.unsafeLoad());
    }

    @Test
    public void whenAlreadyOpenedForRead_thenNoReadConflict() {
        LongRef ref = createLongRef(stm, 100);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal read1 = tx.openForRead(ref, false, pool);

        FatArrayBetaTransaction conflictingTx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal conflictingWrite = conflictingTx.openForWrite(ref, false, pool);
        conflictingWrite.value++;
        conflictingTx.commit(pool);

        Tranlocal read2 = tx.openForRead(ref, false, pool);

        assertSame(read1, read2);
        assertUnlocked(ref.getOrec());
        assertSurplus(1, ref.getOrec());
        assertUpdateBiased(ref.getOrec());
        assertActive(tx);
        assertNull(ref.getLockOwner());
        assertSame(conflictingWrite, ref.unsafeLoad());
    }

    @Test
    public void whenAlreadyOpenedForConstruction() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRef ref = new LongRef(tx);
        LongRefTranlocal constructed = tx.openForConstruction(ref, pool);
        constructed.value = 100;
        Tranlocal read = tx.openForRead(ref, false, pool);

        assertSame(constructed, read);
        assertEquals(100, constructed.value);
        assertFalse(constructed.isCommitted);
        assertFalse(constructed.isPermanent);
        assertLocked(ref.getOrec());
        assertSame(tx, ref.getLockOwner());
        assertSurplus(1, ref.getOrec());
        assertUpdateBiased(ref.getOrec());
        assertActive(tx);
        assertNull(ref.unsafeLoad());
    }

    @Test
    public void whenAlreadyOpenedForConstructionAndLock() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRef ref = new LongRef(tx);
        LongRefTranlocal constructed = tx.openForConstruction(ref, pool);
        constructed.value = 100;
        Tranlocal read = tx.openForRead(ref, true, pool);

        assertSame(constructed, read);
        assertEquals(100, constructed.value);
        assertFalse(constructed.isCommitted);
        assertFalse(constructed.isPermanent);
        assertLocked(ref.getOrec());
        assertSame(tx, ref.getLockOwner());
        assertSurplus(1, ref.getOrec());
        assertUpdateBiased(ref.getOrec());
        assertActive(tx);
        assertNull(ref.unsafeLoad());
    }

    @Test
    public void whenLockedByOther_thenReadConflict() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        FatArrayBetaTransaction other = new FatArrayBetaTransaction(stm);
        other.openForRead(ref, true, pool);

        int oldReadonlyCount = ref.getReadonlyCount();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        try {
            tx.openForRead(ref, false, pool);
            fail();
        } catch (ReadConflict expected) {
        }

        assertLocked(ref.getOrec());
        assertSurplus(1, ref.getOrec());
        assertUpdateBiased(ref.getOrec());
        assertReadonlyCount(oldReadonlyCount, ref);
        assertAborted(tx);
        assertSame(other, ref.getLockOwner());
        assertSame(committed, ref.unsafeLoad());
    }

    @Test
    public void whenReadConflict() {
        LongRef ref1 = createLongRef(stm);
        LongRef ref2 = createLongRef(stm);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal read1 = tx.openForRead(ref1, false, pool);

        FatArrayBetaTransaction conflictingTx = new FatArrayBetaTransaction(stm);
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
        assertNull(ref1.getLockOwner());
        assertSurplus(0, ref1);
        assertUpdateBiased(ref1);

        assertUnlocked(ref2);
        assertNull(ref2.getLockOwner());
        assertSurplus(0, ref2);
        assertUpdateBiased(ref2);
    }

    @Test
    public void conflictCounterIsSetAtFirstRead() {
        LongRef ref = createLongRef(stm, 10);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);

        stm.getGlobalConflictCounter().signalConflict(ref);
        tx.openForRead(ref, false, pool);

        assertEquals(stm.getGlobalConflictCounter().count(), tx.getLocalConflictCounter().get());
        assertActive(tx);
    }

    @Test
    public void conflictCounterIsNotSetWhenAlreadyRead() {
        LongRef ref = createLongRef(stm, 10);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);

        stm.getGlobalConflictCounter().signalConflict(createLongRef(stm));

        tx.openForRead(ref, false, pool);
        long localConflictCount = tx.getLocalConflictCounter().get();
        stm.getGlobalConflictCounter().signalConflict(createLongRef(stm));

        tx.openForRead(ref, false, pool);

        assertEquals(localConflictCount, tx.getLocalConflictCounter().get());
        assertActive(tx);
    }

    @Test
    public void whenContainsUntrackedRead_thenCantRecoverFromUnrealReadConflict() {
        LongRef ref1 = createReadBiasedLongRef(stm, 100);
        LongRef ref2 = createLongRef(stm);

        BetaTransactionConfig config = new BetaTransactionConfig(stm)
                .setReadTrackingEnabled(false);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
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
        assertNull(ref1.getLockOwner());
        assertSurplus(0, ref2);
        assertUnlocked(ref2);
        assertNull(ref2.getLockOwner());
    }

    @Test
    public void whenUnrealConflictThenConflictCounterUpdated() {
        LongRef ref1 = createLongRef(stm);
        LongRef ref2 = createLongRef(stm);
        LongRef ref3 = createLongRef(stm);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);

        stm.getGlobalConflictCounter().signalConflict(createLongRef(stm));

        tx.openForRead(ref1, false, pool);

        //do second read
        stm.getGlobalConflictCounter().signalConflict(createLongRef(stm));
        tx.openForRead(ref2, false, pool);
        assertEquals(stm.getGlobalConflictCounter().count(), tx.getLocalConflictCounter().get());

        //do another read
        stm.getGlobalConflictCounter().signalConflict(createLongRef(stm));
        tx.openForRead(ref3, false, pool);
        assertEquals(stm.getGlobalConflictCounter().count(), tx.getLocalConflictCounter().get());

        assertActive(tx);
    }

    @Test
    public void whenMultipleOpenForReads() {
        LongRef ref1 = BetaStmUtils.createLongRef(stm);
        LongRef ref2 = BetaStmUtils.createLongRef(stm);
        LongRef ref3 = BetaStmUtils.createLongRef(stm);

        BetaTransaction tx = new FatArrayBetaTransaction(stm);
        Tranlocal read1 = tx.openForRead(ref1, false, pool);
        Tranlocal read2 = tx.openForRead(ref2, false, pool);
        Tranlocal read3 = tx.openForRead(ref3, false, pool);

        assertActive(tx);

        assertSame(read1, ref1.unsafeLoad());
        assertSame(read2, ref2.unsafeLoad());
        assertSame(read3, ref3.unsafeLoad());
    }

    @Test
    public void whenPessimisticRead() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        BetaTransactionConfig config = new BetaTransactionConfig(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.Read);
        BetaTransaction tx = new FatArrayBetaTransaction(config);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        assertSame(committed, read);
        assertLocked(ref);
        assertSame(tx, ref.getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertTrue(read.isCommitted);
        assertFalse(read.isPermanent);
    }

    @Test
    public void whenPessimisticWrite() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        BetaTransactionConfig config = new BetaTransactionConfig(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.Write);
        BetaTransaction tx = new FatArrayBetaTransaction(config);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        assertSame(committed, read);
        assertUnlocked(ref);
        assertNull(ref.getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertTrue(read.isCommitted);
        assertFalse(read.isPermanent);
    }


    @Test
    public void whenPessimisticThenNoConflictDetectionNeeded() {
        LongRef ref1 = createLongRef(stm);
        LongRef ref2 = createLongRef(stm);

        BetaTransactionConfig config = new BetaTransactionConfig(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.Read);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
        tx.openForRead(ref1, false, pool);

        long oldLocalConflictCount = tx.getLocalConflictCounter().get();

        stm.getGlobalConflictCounter().signalConflict(createLongRef(stm));

        tx.openForRead(ref2, false, pool);

        assertEquals(oldLocalConflictCount, tx.getLocalConflictCounter().get());
    }

    @Test
    public void whenHasCommutingFunctions() {
        LongRef ref = createLongRef(stm, 10);
        LongRefTranlocal committed = ref.unsafeLoad();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongFunction function = new IncLongFunction();
        tx.commute(ref, pool, function);

        LongRefTranlocal commuting = (LongRefTranlocal) tx.get(ref);

        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        assertSame(commuting, read);
        assertSame(committed, read.read);
        assertFalse(read.isCommuting);
        assertFalse(read.isCommitted);
        assertEquals(11, read.value);
        assertUnlocked(ref);
        assertNull(ref.getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenPrepared_thenPreparedException() {
        LongRef ref = createLongRef(stm);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.prepare(pool);

        try {
            tx.openForRead(ref, false, pool);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        LongRef ref = createLongRef(stm);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.abort(pool);

        try {
            tx.openForRead(ref, false, pool);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        LongRef ref = createLongRef(stm);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.commit(pool);

        try {
            tx.openForRead(ref, false, pool);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertCommitted(tx);
    }
}
