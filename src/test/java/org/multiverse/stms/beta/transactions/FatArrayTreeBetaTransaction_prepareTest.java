package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.WriteConflict;
import org.multiverse.api.functions.IncLongFunction;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactionalobjects.LongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class FatArrayTreeBetaTransaction_prepareTest implements BetaStmConstants {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void whenUnused() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);

        tx.prepare();

        assertPrepared(tx);
    }

    @Test
    public void whenReadIsConflictedByWrite() {
        LongRef ref = createLongRef(stm);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        FatArrayTreeBetaTransaction otherTx = new FatArrayTreeBetaTransaction(stm);
        LongRefTranlocal conflictingWrite = otherTx.openForWrite(ref, false, pool);
        conflictingWrite.value++;
        otherTx.commit();

        tx.prepare(pool);

        assertPrepared(tx);
        assertCommitted(otherTx);
        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());
        assertSame(conflictingWrite, ref.___unsafeLoad());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenReadIsConflictedByLock_thenPrepareSuccess() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        FatArrayTreeBetaTransaction otherTx = new FatArrayTreeBetaTransaction(stm);
        otherTx.openForRead(ref, true, pool);

        tx.prepare(pool);

        assertPrepared(tx);
        assertActive(otherTx);
        assertLocked(ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertSurplus(2, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenUpdate() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        write.value++;
        tx.prepare(pool);

        assertPrepared(tx);

        assertLocked(ref);
        assertSame(tx, ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertFalse(write.isCommitted);
    }

    @Test
    public void whenUpdateAlreadyLockedBySelf() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, true, pool);
        tx.prepare(pool);

        assertPrepared(tx);

        assertLocked(ref);
        assertSame(tx, ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertFalse(write.isCommitted);
    }

    @Test
    public void whenUpdateAndLockedByOther_thenWriteConflict() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        write.value++;

        FatArrayTreeBetaTransaction otherTx = new FatArrayTreeBetaTransaction(stm);
        otherTx.openForRead(ref, true, pool);

        try {
            tx.prepare(pool);
            fail();
        } catch (WriteConflict expected) {
        }

        assertAborted(tx);
        assertActive(otherTx);

        assertLocked(ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertFalse(write.isCommitted);
    }

    @Test
    public void whenOnlyFresh() {

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongRef ref = new LongRef(tx);
        tx.openForConstruction(ref, pool);
        tx.prepare();

        assertPrepared(tx);
        assertSame(tx, ref.___getLockOwner());
        assertLocked(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertNull(ref.___unsafeLoad());
    }

    @Test
    public void whenConflictingWrite() {
        LongRef ref = createLongRef(stm);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        write.value++;

        FatArrayTreeBetaTransaction otherTx = new FatArrayTreeBetaTransaction(stm);
        LongRefTranlocal conflictingWrite = otherTx.openForWrite(ref, false, pool);
        conflictingWrite.value++;
        otherTx.commit();

        try {
            tx.prepare(pool);
            fail();
        } catch (WriteConflict expected) {
        }

        assertAborted(tx);
        assertCommitted(otherTx);

        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());
        assertSame(conflictingWrite, ref.___unsafeLoad());
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertFalse(write.isCommitted);
    }

    @Test
    public void whenAlreadyPrepared() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        write.value++;
        tx.prepare(pool);

        tx.prepare(pool);
        assertPrepared(tx);

        assertLocked(ref);
        assertSame(tx, ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertFalse(write.isCommitted);
    }

    @Test
    public void whenReferenceHasMultipleCommutes() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.commute(ref, pool, IncLongFunction.INSTANCE);
        tx.commute(ref, pool, IncLongFunction.INSTANCE);
        tx.commute(ref, pool, IncLongFunction.INSTANCE);
        tx.prepare();

        LongRefTranlocal commute = (LongRefTranlocal) tx.get(ref);

        assertNotNull(commute);
        assertFalse(commute.isCommuting);
        assertFalse(commute.isCommitted);
        assertSame(ref, commute.owner);
        assertSame(committed, commute.read);
        assertEquals(3, commute.value);
        assertEquals(DIRTY_TRUE, commute.isDirty);
        assertLocked(ref);
        assertSame(tx, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
    }

    @Test
    @Ignore
    public void whenMultipleReferencesHaveCommute() {
        LongRef ref1 = createLongRef(stm, 10);
        LongRef ref2 = createLongRef(stm, 20);
        LongRef ref3 = createLongRef(stm, 30);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.commute(ref1, pool, IncLongFunction.INSTANCE);
        tx.commute(ref2, pool, IncLongFunction.INSTANCE);
        tx.commute(ref3, pool, IncLongFunction.INSTANCE);
        tx.prepare();

        LongRefTranlocal commute1 = (LongRefTranlocal) tx.get(ref1);
        LongRefTranlocal commute2 = (LongRefTranlocal) tx.get(ref1);
        LongRefTranlocal commute3 = (LongRefTranlocal) tx.get(ref1);

        assertNotNull(commute1);
        assertEquals(11, commute1.value);
        assertNotNull(commute1);
        assertEquals(21, commute2.value);
        assertNotNull(commute1);
        assertEquals(31, commute3.value);
    }

    @Test
    public void whenHasCommuteAndNoDirtyCheck() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setDirtyCheckEnabled(false);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);
        tx.commute(ref, pool, IncLongFunction.INSTANCE);
        tx.prepare();

        LongRefTranlocal commute = (LongRefTranlocal) tx.get(ref);

        assertNotNull(commute);
        assertFalse(commute.isCommuting);
        assertFalse(commute.isCommitted);
        assertSame(ref, commute.owner);
        assertSame(committed, commute.read);
        assertEquals(1, commute.value);
        assertEquals(DIRTY_TRUE, commute.isDirty);
        assertLocked(ref);
        assertSame(tx, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenHasCommuteAndDirtyCheck() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setDirtyCheckEnabled(true);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);
        tx.commute(ref, pool, IncLongFunction.INSTANCE);
        tx.prepare();

        LongRefTranlocal commute = (LongRefTranlocal) tx.get(ref);

        assertNotNull(commute);
        assertFalse(commute.isCommuting);
        assertFalse(commute.isCommitted);
        assertSame(ref, commute.owner);
        assertSame(committed, commute.read);
        assertEquals(1, commute.value);
        assertEquals(DIRTY_TRUE, commute.isDirty);
        assertLocked(ref);
        assertSame(tx, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenHasCommuteButLockedByOtherTransaction_thenWriteConflict() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.commute(ref, pool, IncLongFunction.INSTANCE);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, true, pool);

        try {
            tx.prepare();
            fail();
        } catch (WriteConflict expected) {
        }

        assertAborted(tx);

        assertLocked(ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenHasCommuteThatConflicts() {
        LongRef ref = createLongRef(stm);


        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);
        tx.commute(ref, pool, IncLongFunction.INSTANCE);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForWrite(ref, false, pool).value++;
        otherTx.commit();
        LongRefTranlocal committed = ref.___unsafeLoad();

        tx.prepare();

        LongRefTranlocal commute = (LongRefTranlocal) tx.get(ref);

        assertNotNull(commute);
        assertFalse(commute.isCommuting);
        assertFalse(commute.isCommitted);
        assertSame(ref, commute.owner);
        assertSame(committed, commute.read);
        assertEquals(2, commute.value);
        assertEquals(DIRTY_TRUE, commute.isDirty);
        assertLocked(ref);
        assertSame(tx, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
    }

    @Test
     public void whenWriteSkewStillPossibleWithWriteSkewEnabled() {
         LongRef ref1 = createLongRef(stm, 0);
         LongRef ref2 = createLongRef(stm, 0);

         BetaTransaction tx1 = new FatArrayTreeBetaTransaction(stm);
         tx1.openForWrite(ref1, false, pool).value++;
         tx1.openForRead(ref2, false, pool);

         BetaTransaction tx2 = new FatArrayTreeBetaTransaction(stm);
         tx2.openForRead(ref1, false, pool);
         tx2.openForWrite(ref2, false, pool).value++;

         tx1.prepare(pool);
         tx2.prepare(pool);
     }

     @Test
     public void whenWriteSkewNotPossibleWithoutWriteSkewDisabled() {
         LongRef ref1 = createLongRef(stm, 0);
         LongRef ref2 = createLongRef(stm, 0);

         BetaTransaction tx1 = new FatArrayTreeBetaTransaction(stm);
         tx1.openForWrite(ref1, false, pool).value++;
         tx1.openForRead(ref2, false, pool);

         BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                 .setWriteSkewAllowed(false);
         BetaTransaction tx2 = new FatArrayTreeBetaTransaction(config);
         tx2.openForRead(ref1, false, pool);
         tx2.openForWrite(ref2, false, pool).value++;

         tx1.prepare(pool);

         try {
             tx2.prepare(pool);
             fail();
         } catch (WriteConflict expected) {
         }

         assertAborted(tx2);
     }


    @Test
    public void whenAbortOnly() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.setAbortOnly();

        try {
            tx.prepare();
            fail();
        } catch (WriteConflict conflict) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.commit(pool);

        try {
            tx.prepare(pool);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertCommitted(tx);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.abort(pool);

        try {
            tx.prepare(pool);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertAborted(tx);
    }
}
