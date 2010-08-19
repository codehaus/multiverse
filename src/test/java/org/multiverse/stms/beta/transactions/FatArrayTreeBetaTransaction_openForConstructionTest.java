package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.LongRefTranlocal;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class FatArrayTreeBetaTransaction_openForConstructionTest {

    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void whenSuccess() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongRef ref = new LongRef(tx);

        LongRefTranlocal write = tx.openForConstruction(ref, pool);

        assertActive(tx);
        assertNotNull(write);
        assertEquals(0, write.value);
        assertSame(ref, write.owner);
        assertNull(write.read);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(tx, ref.___getLockOwner());
        assertLocked(ref);
        assertSurplus(1, ref);
        assertAttached(tx, write);
    }

    @Test
    public void whenNullRef_thenNullPointerException() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);

        try {
            tx.openForConstruction((LongRef) null, pool);
            fail();
        } catch (NullPointerException expected) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenAlreadyCommitted_thenIllegalArgumentException() {
        LongRef ref = createLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);

        try {
            tx.openForConstruction(ref, pool);
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
    public void whenAlreadyOpenedForConstruction() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongRef ref = new LongRef(tx);

        LongRefTranlocal write1 = tx.openForConstruction(ref, pool);
        LongRefTranlocal write2 = tx.openForConstruction(ref, pool);

        assertSame(write1, write2);
        assertActive(tx);
        assertNotNull(write2);
        assertEquals(0, write2.value);
        assertSame(ref, write2.owner);
        assertNull(write2.read);
        assertFalse(write2.isCommitted);
        assertFalse(write2.isPermanent);
        assertSame(tx, ref.___getLockOwner());
        assertLocked(ref);
        assertSurplus(1, ref);
        assertAttached(tx, write1);
    }

    @Test
    public void whenAlreadyOpenedForReading_thenIllegalArgumentException() {
        LongRef ref = createLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.openForRead(ref, false, pool);

        try {
            tx.openForConstruction(ref, pool);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        //todo: this should be aborted.
        assertActive(tx);
        assertSame(committed, ref.___unsafeLoad());
        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenAlreadyOpenedForWrite_thenIllegalArgumentException() {
        LongRef ref = createLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.openForWrite(ref, false, pool);

        try {
            tx.openForConstruction(ref, pool);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        //todo: this should be aborted.
        assertActive(tx);
        assertSame(committed, ref.___unsafeLoad());
        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenReadonly_thenReadonlyException() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setReadonly(true);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);

        try {
            tx.openForConstruction(ref, pool);
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
        LongRef ref1 = createLongRef(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.Read);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);
        LongRefTranlocal read1 = tx.openForRead(ref1, false, pool);

        long oldLocalConflictCount = tx.getLocalConflictCounter().get();

        stm.getGlobalConflictCounter().signalConflict(createLongRef(stm));
        LongRef ref2 = new LongRef(tx);
        LongRefTranlocal constructed2 = tx.openForConstruction(ref2, pool);

        assertEquals(oldLocalConflictCount, tx.getLocalConflictCounter().get());

        assertActive(tx);
        assertAttached(tx, read1);
        assertAttached(tx, constructed2);
    }

    @Test
    public void conflictCounterIsNotReset() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        long oldConflictCount = tx.getLocalConflictCounter().get();
        LongRef ref = new LongRef(tx);

        stm.getGlobalConflictCounter().signalConflict(createLongRef(stm));
        LongRefTranlocal constructed = tx.openForConstruction(ref, pool);

        assertEquals(oldConflictCount, tx.getLocalConflictCounter().get());
        assertActive(tx);
        assertAttached(tx, constructed);
    }

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        LongRef ref = createLongRef(stm);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.prepare(pool);

        try {
            tx.openForConstruction(ref, pool);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        LongRef ref = createLongRef(stm);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.abort(pool);

        try {
            tx.openForConstruction(ref, pool);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        LongRef ref = createLongRef(stm);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.commit(pool);

        try {
            tx.openForConstruction(ref, pool);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertCommitted(tx);
    }
}
