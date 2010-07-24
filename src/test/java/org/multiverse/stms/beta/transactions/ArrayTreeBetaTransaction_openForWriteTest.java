package org.multiverse.stms.beta.transactions;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadConflict;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.ObjectPool;
import org.multiverse.stms.beta.StmUtils;
import org.multiverse.stms.beta.orec.Orec;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.LongRefTranlocal;
import org.multiverse.stms.beta.refs.Tranlocal;

import static junit.framework.Assert.assertSame;
import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.StmUtils.createLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class ArrayTreeBetaTransaction_openForWriteTest {

    private BetaStm stm;
    private ObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new ObjectPool();
    }

    @Test
    public void whenManyItems_readonly() {
        whenManyItems(true);
    }

    @Test
    public void whenManyItems_update() {
        whenManyItems(false);
    }

    public void whenManyItems(boolean reading) {
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);

        int refCount = 10000;
        LongRef[] refs = new LongRef[refCount];
        LongRefTranlocal[] tranlocals = new LongRefTranlocal[refCount];
        for (int k = 0; k < refCount; k++) {
            LongRef ref = createLongRef(stm);
            refs[k] = ref;
            tranlocals[k] = reading ? tx.openForRead(ref, false, pool) : tx.openForWrite(ref, false, pool);
        }

        assertEquals(refCount, tx.size());

        System.out.println("everything inserted");
        System.out.println("usage percentage: " + (100 * tx.getUsage()));

        for (int k = 0; k < refCount; k++) {
            LongRef ref = refs[k];
            Tranlocal found = reading ? tx.openForRead(ref, false, pool) : tx.openForWrite(ref, false, pool);
            assertSame(ref, found.owner);
            assertSame("tranlocal is incorrect at " + k, tranlocals[k], found);
        }
    }

    @Test
    public void whenNull_thenNullPointerException() {
        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);

        try {
            tx.openForWrite((LongRef) null, false, pool);
            fail();
        } catch (NullPointerException ex) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenReadonly_thenAbortedAndReadonlyException() {
        LongRef ref = createLongRef(stm, 0);
        Tranlocal committed = ref.unsafeLoad();

        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(
                new BetaTransactionConfig(stm).setReadonly(true));
        try {
            tx.openForWrite(ref, false, pool);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertAborted(tx);
        assertSame(committed, ref.unsafeLoad());
    }

    @Test
    public void whenReadonlyAndAlreadyOpenedForRead_thenReadonlyException() {
        LongRef ref = createLongRef(stm, 0);

        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(
                new BetaTransactionConfig(stm).setReadonly(true));
        tx.openForRead(ref, false, pool);

        try {
            tx.openForWrite(ref, false, pool);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenFresh() {
        LongRef ref = createLongRef(stm, 0);
        Tranlocal committed = ref.unsafeLoad();

        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        Tranlocal tranlocal = tx.openForWrite(ref, false, pool);
        assertFalse(tranlocal.isCommitted());
        assertSame(committed, tranlocal.read);
        assertSame(tranlocal.owner, ref);
    }

    @Test
    public void whenAlreadyOpenedForRead() {
        LongRef ref = createLongRef(stm, 0);

        BetaTransaction tx = stm.start();
        Tranlocal read = tx.openForRead(ref, false, pool);
        Tranlocal write = tx.openForWrite(ref, false, pool);

        assertNotSame(read, write);
        assertFalse(write.isCommitted());
        assertSame(write.owner, ref);
        assertSame(ref, write.owner);
    }

    @Test
    public void whenAlreadyOpenedForWrite() {
        LongRef ref = createLongRef(stm, 0);

        Tranlocal committed = ref.unsafeLoad();

        BetaTransaction tx = stm.start();
        Tranlocal write1 = tx.openForWrite(ref, false, pool);
        Tranlocal write2 = tx.openForWrite(ref, false, pool);

        assertSame(write1, write2);
        assertFalse(write2.isCommitted());
        assertSame(committed, write1.read);
    }

    @Test
    public void whenAlreadyOpenedForReadThenNoReadConflict() {
        LongRef ref = createLongRef(stm, 0);

        BetaTransaction tx1 = stm.start();
        Tranlocal read = tx1.openForRead(ref, false, pool);

        BetaTransaction tx2 = stm.start();
        tx2.openForWrite(ref, false, pool);
        tx2.commit(pool);

        Tranlocal write = tx1.openForWrite(ref, false, pool);
        assertNotNull(write);
        assertFalse(write.isCommitted());
        assertSame(read, write.read);
        assertSame(ref, write.owner);
    }

    @Test
    public void whenAlreadyOpenedForWriteThenNoReadConflict() {
        LongRef ref = createLongRef(stm, 0);

        BetaTransaction tx1 = stm.start();
        Tranlocal write1 = tx1.openForWrite(ref, false, pool);

        BetaTransaction tx2 = stm.start();
        tx2.openForWrite(ref, false, pool);
        tx2.commit(pool);

        Tranlocal write2 = tx1.openForWrite(ref, false, pool);
        assertSame(write2, write1);
        assertFalse(write2.isCommitted());
    }

    @Test
    public void whenLocked() {
        LongRef ref = StmUtils.createLongRef(stm);
        Orec orec = ref.getOrec();
        orec.arrive(1);
        orec.tryUpdateLock(1);

        BetaTransaction tx1 = stm.start();

        try {
            tx1.openForWrite(ref, false, pool);
            fail();
        } catch (ReadConflict expected) {
        }

        assertLocked(orec);
    }

    @Test
    public void whenLockedAfterOpenedForWrite_thenNoProblems() {
        LongRef ref = StmUtils.createLongRef(stm);

        BetaTransaction tx1 = new ArrayTreeBetaTransaction(stm);
        Tranlocal open1 = tx1.openForWrite(ref, false, pool);

        BetaTransaction tx2 = stm.start();
        tx2.openForWrite(ref, true, pool);

        Tranlocal open2 = tx1.openForWrite(ref, false, pool);
        assertSame(open1, open2);
    }

    @Test
    public void whenPessimisticReadEnabled() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        BetaTransactionConfig config = new BetaTransactionConfig(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.Read);
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(config);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);

        assertNotSame(committed, write);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(committed, write.read);
        assertLocked(ref);
        Assert.assertSame(tx, ref.getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenPessimisticWriteEnabled() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        BetaTransactionConfig config = new BetaTransactionConfig(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.Write);
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(config);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);

        assertNotSame(committed, write);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(committed, write.read);
        assertLocked(ref);
        Assert.assertSame(tx, ref.getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenReadConflict() {
        LongRef ref1 = createLongRef(stm);
        LongRef ref2 = createLongRef(stm);

        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        LongRefTranlocal read1 = tx.openForRead(ref1, false, pool);

        ArrayTreeBetaTransaction conflictingTx = new ArrayTreeBetaTransaction(stm);
        conflictingTx.openForWrite(ref1, false, pool).value++;
        conflictingTx.openForWrite(ref2, false, pool).value++;
        conflictingTx.commit(pool);

        try {
            tx.openForWrite(ref2, false, pool);
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
    public void whenAlreadyOpenedForConstruction() {
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        LongRef ref = new LongRef(tx);
        LongRefTranlocal constructed = tx.openForConstruction(ref, pool);
        constructed.value = 100;

        LongRefTranlocal write = tx.openForWrite(ref, false, pool);

        assertNotNull(write);
        assertSame(constructed, write);
        assertEquals(100, write.value);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(tx, ref.getLockOwner());
        assertLocked(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertActive(tx);
    }

    @Test
    public void whenConstructedAndLock() {
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        LongRef ref = new LongRef(tx);
        LongRefTranlocal constructed = tx.openForConstruction(ref, pool);
        constructed.value = 100;

        LongRefTranlocal write = tx.openForWrite(ref, true, pool);

        assertNotNull(write);
        assertSame(constructed, write);
        assertEquals(100, write.value);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(tx, ref.getLockOwner());
        assertLocked(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertActive(tx);
    }

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        LongRef ref = createLongRef(stm);

        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        tx.prepare(pool);

        try {
            tx.openForWrite(ref, false, pool);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertPrepared(tx);
    }

    @Test
    public void whenAborted_thenIllegalStateException() {
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        tx.abort(pool);

        LongRef ref = StmUtils.createLongRef(stm);

        try {
            tx.openForWrite(ref, true, pool);
            fail();
        } catch (IllegalStateException expected) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenCommitted_thenIllegalStateException() {
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        tx.commit(pool);

        LongRef ref = StmUtils.createLongRef(stm);

        try {
            tx.openForWrite(ref, true, pool);
            fail();
        } catch (IllegalStateException expected) {
        }

        assertCommitted(tx);
    }
}
