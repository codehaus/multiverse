package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.api.exceptions.SpeculativeConfigurationError;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.LongRefTranlocal;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class FatArrayBetaTransaction_openForConstructionTest {

    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    @Ignore
    public void whenFresh() {

    }

    @Test
    public void whenOverflowing() {
        BetaTransactionConfig config = new BetaTransactionConfig(stm, 3);
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);

        LongRef ref1 = new LongRef(tx);
        LongRef ref2 = new LongRef(tx);
        LongRef ref3 = new LongRef(tx);
        LongRef ref4 = new LongRef(tx);

        tx.openForConstruction(ref1, pool);
        tx.openForConstruction(ref2, pool);
        tx.openForConstruction(ref3, pool);
        try {
            tx.openForConstruction(ref4, pool);
            fail();
        } catch (SpeculativeConfigurationError expected) {
        }

        assertAborted(tx);
        assertEquals(4, config.getSpeculativeConfig().getMinimalLength());
    }

    @Test
    public void whenSuccess() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRef ref = new LongRef(tx);
        LongRefTranlocal write = tx.openForConstruction(ref, pool);

        assertActive(tx);
        assertNeedsRealClose(tx);
        assertNotNull(write);
        assertEquals(0, write.value);
        assertSame(ref, write.owner);
        assertNull(write.read);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(tx, ref.getLockOwner());
        assertLocked(ref);
        assertSurplus(1, ref);
    }

    @Test
    public void whenAlreadyOpenedForConstruction() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRef ref = new LongRef(tx);
        LongRefTranlocal construction1 = tx.openForConstruction(ref, pool);
        LongRefTranlocal construction2 = tx.openForConstruction(ref, pool);

        assertActive(tx);
        assertNeedsRealClose(tx);
        assertSame(construction1, construction2);
        assertNotNull(construction1);
        assertEquals(0, construction1.value);
        assertSame(ref, construction1.owner);
        assertNull(construction1.read);
        assertFalse(construction1.isCommitted);
        assertFalse(construction1.isPermanent);
        assertSame(tx, ref.getLockOwner());
        assertLocked(ref);
        assertSurplus(1, ref);
    }

    @Test
    public void whenNullRef_thenNullPointerException() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);

        try {
            tx.openForConstruction((LongRef) null, pool);
            fail();
        } catch (NullPointerException expected) {
        }

        assertNeedsNoRealClose(tx);
        assertAborted(tx);
    }

    @Test
    public void whenAlreadyCommitted_thenIllegalArgumentException() {
        LongRef ref = createLongRef(stm, 100);
        LongRefTranlocal committed = ref.unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);

        try {
            tx.openForConstruction(ref, pool);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertAborted(tx);
        assertNeedsNoRealClose(tx);
        assertSame(committed, ref.unsafeLoad());
        assertUnlocked(ref);
        assertNull(ref.getLockOwner());
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenAlreadyOpenedForReading_thenIllegalArgumentException() {
        LongRef ref = createLongRef(stm, 100);
        LongRefTranlocal committed = ref.unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.openForRead(ref, false, pool);

        try {
            tx.openForConstruction(ref, pool);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertNeedsRealClose(tx);
        assertAborted(tx);
        assertSame(committed, ref.unsafeLoad());
        assertUnlocked(ref);
        assertNull(ref.getLockOwner());
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenAlreadyOpenedForWrite_thenIllegalArgumentException() {
        LongRef ref = createLongRef(stm, 100);
        LongRefTranlocal committed = ref.unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.openForWrite(ref, false, pool);

        try {
            tx.openForConstruction(ref, pool);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertAborted(tx);
        assertNeedsRealClose(tx);
        assertSame(committed, ref.unsafeLoad());
        assertUnlocked(ref);
        assertNull(ref.getLockOwner());
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenReadonly_thenReadonlyException() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        BetaTransactionConfig config = new BetaTransactionConfig(stm)
                .setReadonly(true);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);

        try {
            tx.openForConstruction(ref, pool);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertNeedsNoRealClose(tx);
        assertUnlocked(ref);
        assertSame(committed, ref.unsafeLoad());
        assertNull(ref.getLockOwner());
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenPessimisticThenNoConflictDetectionNeeded() {
        LongRef ref1 = createLongRef(stm);

        BetaTransactionConfig config = new BetaTransactionConfig(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.Read);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
        tx.openForRead(ref1, false, pool);

        long oldLocalConflictCount = tx.getLocalConflictCounter().get();

        stm.getGlobalConflictCounter().signalConflict(createLongRef(stm));
        LongRef ref2 = new LongRef(tx);
        tx.openForConstruction(ref2, pool);

        assertEquals(oldLocalConflictCount, tx.getLocalConflictCounter().get());
        assertNeedsRealClose(tx);
    }

    @Test
    public void conflictCounterIsNotReset() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        long oldConflictCount = tx.getLocalConflictCounter().get();
        LongRef ref = new LongRef(tx);

        stm.getGlobalConflictCounter().signalConflict(createLongRef(stm));
        tx.openForConstruction(ref, pool);

        assertEquals(oldConflictCount, tx.getLocalConflictCounter().get());
        assertActive(tx);
        assertNeedsRealClose(tx);
    }

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        LongRef ref = createLongRef(stm);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.prepare(pool);

        try {
            tx.openForConstruction(ref, pool);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertAborted(tx);
        assertNeedsNoRealClose(tx);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        LongRef ref = createLongRef(stm);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.abort(pool);

        try {
            tx.openForConstruction(ref, pool);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertAborted(tx);
        assertNeedsNoRealClose(tx);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        LongRef ref = createLongRef(stm);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.commit(pool);

        try {
            tx.openForConstruction(ref, pool);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertCommitted(tx);
        assertNeedsNoRealClose(tx);
    }
}
