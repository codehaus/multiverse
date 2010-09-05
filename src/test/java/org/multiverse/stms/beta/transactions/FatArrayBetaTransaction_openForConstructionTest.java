package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.api.exceptions.SpeculativeConfigurationError;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class FatArrayBetaTransaction_openForConstructionTest implements BetaStmConstants {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    @Ignore
    public void whenFresh() {

    }

    @Test
    public void whenOverflowing() {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, 3);
        config.init();
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);

        BetaLongRef ref1 = new BetaLongRef(tx);
        BetaLongRef ref2 = new BetaLongRef(tx);
        BetaLongRef ref3 = new BetaLongRef(tx);
        BetaLongRef ref4 = new BetaLongRef(tx);

        tx.openForConstruction(ref1);
        tx.openForConstruction(ref2);
        tx.openForConstruction(ref3);
        try {
            tx.openForConstruction(ref4);
            fail();
        } catch (SpeculativeConfigurationError expected) {
        }

        assertAborted(tx);
        assertEquals(4, config.getSpeculativeConfig().getMinimalLength());
    }

    @Test
    public void whenSuccess() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        BetaLongRef ref = new BetaLongRef(tx);
        LongRefTranlocal write = tx.openForConstruction(ref);

        assertActive(tx);
        assertAttached(tx, write);
        assertNotNull(write);
        assertEquals(0, write.value);
        assertSame(ref, write.owner);
        assertNull(write.read);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(tx, ref.___getLockOwner());
        assertLocked(ref);
        assertSurplus(1, ref);
        assertEquals(DIRTY_TRUE, write.isDirty);
    }

    @Test
    public void whenAlreadyOpenedForConstruction() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        BetaLongRef ref = new BetaLongRef(tx);
        LongRefTranlocal construction1 = tx.openForConstruction(ref);
        LongRefTranlocal construction2 = tx.openForConstruction(ref);

        assertActive(tx);
        assertAttached(tx,construction1);
        assertSame(construction1, construction2);
        assertNotNull(construction1);
        assertEquals(0, construction1.value);
        assertSame(ref, construction1.owner);
        assertNull(construction1.read);
        assertFalse(construction1.isCommitted);
        assertFalse(construction1.isPermanent);
        assertSame(tx, ref.___getLockOwner());
        assertLocked(ref);
        assertSurplus(1, ref);
        assertEquals(DIRTY_TRUE, construction1.isDirty);
    }

    @Test
    public void whenNullRef_thenNullPointerException() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);

        try {
            tx.openForConstruction((BetaLongRef) null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenAlreadyCommitted_thenIllegalArgumentException() {
        BetaLongRef ref = createLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);

        try {
            tx.openForConstruction(ref);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertAborted(tx);
        assertSame(committed, ref.___unsafeLoad());
        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenAlreadyOpenedForReading_thenIllegalArgumentException() {
        BetaLongRef ref = createLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.openForRead(ref, false);

        try {
            tx.openForConstruction(ref);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertAborted(tx);
        assertSame(committed, ref.___unsafeLoad());
        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenAlreadyOpenedForWrite_thenIllegalArgumentException() {
        BetaLongRef ref = createLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.openForWrite(ref, false);

        try {
            tx.openForConstruction(ref);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertAborted(tx);
        assertSame(committed, ref.___unsafeLoad());
        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenReadonly_thenReadonlyException() {
        BetaLongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setReadonly(true);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);

        try {
            tx.openForConstruction(ref);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertAborted(tx);
        assertUnlocked(ref);
        assertSame(committed, ref.___unsafeLoad());
        assertNull(ref.___getLockOwner());
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenPessimisticThenNoConflictDetectionNeeded() {
        BetaLongRef ref1 = createLongRef(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.Read);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
        tx.openForRead(ref1, false);

        long oldLocalConflictCount = tx.getLocalConflictCounter().get();

        stm.getGlobalConflictCounter().signalConflict(createLongRef(stm));
        BetaLongRef ref2 = new BetaLongRef(tx);
        tx.openForConstruction(ref2);

        assertEquals(oldLocalConflictCount, tx.getLocalConflictCounter().get());        
    }

    @Test
    public void conflictCounterIsNotReset() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        long oldConflictCount = tx.getLocalConflictCounter().get();
        BetaLongRef ref = new BetaLongRef(tx);

        stm.getGlobalConflictCounter().signalConflict(createLongRef(stm));
        tx.openForConstruction(ref);

        assertEquals(oldConflictCount, tx.getLocalConflictCounter().get());
        assertActive(tx);
    }

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        BetaLongRef ref = createLongRef(stm);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.prepare();

        try {
            tx.openForConstruction(ref);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        BetaLongRef ref = createLongRef(stm);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.abort();

        try {
            tx.openForConstruction(ref);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        BetaLongRef ref = createLongRef(stm);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.commit();

        try {
            tx.openForConstruction(ref);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertCommitted(tx);
    }
}
