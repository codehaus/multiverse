package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.ObjectPool;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.LongRefTranlocal;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.StmUtils.createLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class MonoBetaTransaction_openForConstructionTest {
    private BetaStm stm;
    private ObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new ObjectPool();
    }

    @Test
    public void whenSuccess() {
        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        LongRef ref = new LongRef(tx);

        LongRefTranlocal write = tx.openForConstruction(ref, pool);

        assertActive(tx);
        assertNotNull(write);
        assertEquals(0, write.value);
        assertSame(ref, write.owner);
        assertNull(write.read);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(tx,ref.getLockOwner());
        assertLocked(ref);
        assertSurplus(1, ref);
    }

    

    @Test
    public void whenNullRef_thenNullPointerException() {
        MonoBetaTransaction tx = new MonoBetaTransaction(stm);

        try {
            tx.openForConstruction((LongRef) null, pool);
            fail();
        } catch (NullPointerException expected) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenAlreadyCommitted() {
        LongRef ref = createLongRef(stm, 100);
        LongRefTranlocal committed = ref.unsafeLoad();

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);

        try {
            tx.openForConstruction(ref, pool);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertActive(tx);
        assertSame(committed, ref.unsafeLoad());
        assertUnlocked(ref);
        assertNull(ref.getLockOwner());
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenAlreadyOpenedForConstruction() {
        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
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
        assertSame(tx,ref.getLockOwner());
        assertLocked(ref);
        assertSurplus(1, ref);
    }

    @Test
    public void whenAlreadyOpenedForReading_thenIllegalArgumentException() {
        LongRef ref = createLongRef(stm, 100);
        LongRefTranlocal committed = ref.unsafeLoad();

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        tx.openForRead(ref, false, pool);

        try {
            tx.openForConstruction(ref, pool);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertActive(tx);
        assertSame(committed, ref.unsafeLoad());
        assertUnlocked(ref);
        assertNull(ref.getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenAlreadyOpenedForWrite_thenIllegalArgumentException() {
        LongRef ref = createLongRef(stm, 100);
        LongRefTranlocal committed = ref.unsafeLoad();

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        tx.openForWrite(ref, false, pool);

        try {
            tx.openForConstruction(ref, pool);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertActive(tx);
        assertSame(committed, ref.unsafeLoad());
        assertUnlocked(ref);
        assertNull(ref.getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenReadonly_thenReadonlyException() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        BetaTransactionConfig config = new BetaTransactionConfig(stm)
                .setReadonly(true);

        MonoBetaTransaction tx = new MonoBetaTransaction(config);

        try {
            tx.openForConstruction(ref, pool);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertUnlocked(ref);
        assertSame(committed,ref.unsafeLoad());
        assertNull(ref.getLockOwner());
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        LongRef ref = createLongRef(stm);

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        tx.prepare(pool);

        try {
            tx.openForConstruction(ref, pool);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertPrepared(tx);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        LongRef ref = createLongRef(stm);

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
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

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        tx.commit(pool);

        try {
            tx.openForConstruction(ref, pool);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertCommitted(tx);
    }
}
