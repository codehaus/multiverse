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

        Tranlocal read1 = tx.openForRead(ref1, false, pool);

        assertEquals(tx.getLocalConflictCounter().get(), stm.getGlobalConflictCounter().count());

        Tranlocal read2 = tx.openForRead(ref2, false, pool);

        assertEquals(tx.getLocalConflictCounter().get(), stm.getGlobalConflictCounter().count());
        assertActive(tx);
        assertAttached(tx, read1);
        assertAttached(tx, read2);
    }

    @Test
    public void whenOverflowing() {
        LongRef ref1 = createLongRef(stm);
        LongRef ref2 = createLongRef(stm);
        LongRef ref3 = createLongRef(stm);
        LongRef ref4 = createLongRef(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, 3);
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
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setBlockingAllowed(false)
                .setReadTrackingEnabled(false);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
        LongRefTranlocal tranlocal = tx.openForRead(ref, false, pool);

        assertActive(tx);
        assertSame(committed, tranlocal);
        assertNull(tx.get(ref));
        assertTrue((Boolean) getField(tx, "hasReads"));
        assertTrue((Boolean) getField(tx, "hasUntrackedReads"));
        assertNotAttached(tx, ref);
    }
    
     @Test
    public void whenReadBiasedAndNoReadTrackingAndLock_thenAttached() {
        LongRef ref = createReadBiasedLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setBlockingAllowed(false)
                .setReadTrackingEnabled(false);
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
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
    public void whenReadBiased() {
        LongRef ref = createReadBiasedLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        assertActive(tx);
        assertSame(committed, read);
        assertEquals(100, read.value);
        assertTrue(read.isPermanent);
        assertTrue(read.isCommitted);
        assertUnlocked(ref.___getOrec());
        assertSurplus(1, ref.___getOrec());
        assertReadBiased(ref.___getOrec());
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertAttached(tx, read);
    }

    @Test
    public void whenUpdateBiased() {
        LongRef ref = createLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        assertSame(committed, read);
        assertEquals(100, read.value);
        assertFalse(read.isPermanent);
        assertTrue(read.isCommitted);
        assertUnlocked(ref);
        assertSurplus(1, ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
        assertSame(committed, ref.___unsafeLoad());
        assertNull(ref.___getLockOwner());
        assertActive(tx);
        assertAttached(tx, read);
    }

    @Test
    public void whenReadBiasedAndLock() {
        LongRef ref = createReadBiasedLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, true, pool);

        assertSame(committed, read);
        assertEquals(100, read.value);
        assertTrue(read.isPermanent);
        assertTrue(read.isCommitted);
        assertLocked(ref.___getOrec());
        assertSurplus(1, ref.___getOrec());
        assertReadBiased(ref.___getOrec());
        assertSame(tx, ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertActive(tx);
        assertAttached(tx, read);
    }

    @Test
    public void whenUpdateBiasedAndLock() {
        LongRef ref = createLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, true, pool);

        assertSame(committed, read);
        assertEquals(100, read.value);
        assertFalse(read.isPermanent);
        assertTrue(read.isCommitted);
        assertLocked(ref.___getOrec());
        assertSurplus(1, ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
        assertSame(tx, ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertActive(tx);
        assertAttached(tx, read);
    }

    @Test
    public void whenAlreadyOpenedForWrite() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        write.value = 100;
        Tranlocal read = tx.openForRead(ref, false, pool);

        assertSame(write, read);
        assertEquals(100, write.value);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertUnlocked(ref.___getOrec());
        assertSurplus(1, ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
        assertActive(tx);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertAttached(tx, read);
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
        assertUnlocked(ref.___getOrec());
        assertSurplus(1, ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
        assertActive(tx);
        assertNull(ref.___getLockOwner());
        assertSame(conflictingWrite, ref.___unsafeLoad());
        assertAttached(tx, read);
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
        assertUnlocked(ref.___getOrec());
        assertSurplus(1, ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
        assertActive(tx);
        assertNull(ref.___getLockOwner());
        assertSame(conflictingWrite, ref.___unsafeLoad());
        assertAttached(tx, read1);
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
        assertLocked(ref.___getOrec());
        assertSame(tx, ref.___getLockOwner());
        assertSurplus(1, ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
        assertActive(tx);
        assertNull(ref.___unsafeLoad());
        assertAttached(tx, read);
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
        assertLocked(ref.___getOrec());
        assertSame(tx, ref.___getLockOwner());
        assertSurplus(1, ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
        assertActive(tx);
        assertNull(ref.___unsafeLoad());
        assertAttached(tx, read);
    }

    @Test
    public void whenLockedByOther_thenReadConflict() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction other = new FatArrayBetaTransaction(stm);
        other.openForRead(ref, true, pool);

        int oldReadonlyCount = ref.___getReadonlyCount();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        try {
            tx.openForRead(ref, false, pool);
            fail();
        } catch (ReadConflict expected) {
        }

        assertLocked(ref.___getOrec());
        assertSurplus(1, ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
        assertReadonlyCount(oldReadonlyCount, ref);
        assertAborted(tx);
        assertSame(other, ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenReadConflict() {
        LongRef ref1 = createLongRef(stm);
        LongRef ref2 = createLongRef(stm);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal read1 = tx.openForRead(ref1, false, pool);

        FatArrayBetaTransaction conflictingTx = new FatArrayBetaTransaction(stm);
        conflictingTx.openForWrite(ref1, false, pool).value++;
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

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
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
        assertNull(ref1.___getLockOwner());
        assertSurplus(0, ref2);
        assertUnlocked(ref2);
        assertNull(ref2.___getLockOwner());
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
        assertSame(read1, ref1.___unsafeLoad());
        assertSame(read2, ref2.___unsafeLoad());
        assertSame(read3, ref3.___unsafeLoad());
        assertAttached(tx, read1);
        assertAttached(tx, read2);
        assertAttached(tx, read3);
    }

    @Test
    public void whenPessimisticRead() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.Read);
        BetaTransaction tx = new FatArrayBetaTransaction(config);
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
    public void whenPessimisticThenNoConflictDetectionNeeded() {
        LongRef ref1 = createLongRef(stm);
        LongRef ref2 = createLongRef(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
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
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
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
    public void whenHasCommutingFunctionsAndLocked() {
        LongRef ref = createLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction otherTx = new FatArrayBetaTransaction(stm);
        otherTx.openForRead(ref, true, pool);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongFunction function = new IncLongFunction();
        tx.commute(ref, pool, function);

        try {
            tx.openForRead(ref, false, pool);
            fail();
        } catch (ReadConflict expected) {

        }

        assertAborted(tx);
        assertSame(otherTx, ref.lockOwner);
        assertLocked(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertSame(committed, ref.___unsafeLoad());
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
