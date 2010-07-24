package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.ObjectPool;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.LongRefTranlocal;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.StmUtils.createLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class MonoBetaTransaction_abortTest {
    private BetaStm stm;
    private ObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new ObjectPool();
    }

    @Test
    public void whenUnused() {
        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        tx.abort(pool);

        assertAborted(tx);
    }

    @Test
    public void whenContainsReadBiasedRead() {
        LongRef ref = createReadBiasedLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        tx.openForRead(ref, false, pool);
        tx.abort(pool);

        assertAborted(tx);

        assertNull(ref.getLockOwner());
        assertSame(committed, ref.unsafeLoad());
        assertUnlocked(ref.getOrec());
        assertReadBiased(ref.getOrec());
        assertReadonlyCount(0, ref.getOrec());
        //once arrived, a depart will not be called on a readbiased tranlocal
        assertSurplus(1, ref.getOrec());
    }

    @Test
    public void whenContainsNormalRead() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        tx.openForRead(ref, false, pool);
        tx.abort();

        assertAborted(tx);
        assertUnlocked(ref.getOrec());
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertNull(ref.getLockOwner());
        assertSame(committed, ref.unsafeLoad());
        assertFalse(committed.isPermanent);
    }

    @Test
    public void whenContainsLockedNormalRead() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        tx.openForRead(ref, true, pool);
        tx.abort();

        assertAborted(tx);
        assertUnlocked(ref.getOrec());
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertNull(ref.getLockOwner());
        assertSame(committed, ref.unsafeLoad());
        assertFalse(committed.isPermanent);
    }

    @Test
    public void whenContainsUnlockedWrite() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        tx.openForWrite(ref, false, pool);
        tx.abort();

        assertAborted(tx);
        assertUnlocked(ref.getOrec());
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertNull(ref.getLockOwner());
        assertSame(committed, ref.unsafeLoad());
        assertFalse(committed.isPermanent);
    }

    @Test
    public void whenContainsLockedWrite() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        tx.openForWrite(ref, true, pool);
        tx.abort();

        assertAborted(tx);
        assertUnlocked(ref.getOrec());
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertNull(ref.getLockOwner());
        assertSame(committed, ref.unsafeLoad());
        assertFalse(committed.isPermanent);
    }

    @Test
    public void whenContainsConstructed_thenItemRemainsLocked() {
        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        LongRef ref = new LongRef(tx);
        LongRefTranlocal write = tx.openForConstruction(ref, pool);
        tx.abort(pool);

        assertAborted(tx);

        assertSame(tx,ref.getLockOwner());
        assertLocked(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertNull(ref.unsafeLoad());
        assertFalse(write.isPermanent);
        assertFalse(write.isCommitted);
    }

    @Test
    public void whenPrepared(){
         MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        tx.prepare(pool);

        tx.abort(pool);
        assertAborted(tx);
    }

    @Test
    public void whenAborted_thenIgnored() {
        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        tx.abort(pool);

        tx.abort(pool);
        assertAborted(tx);
    }

    @Test
    public void whenCommitted_DeadTransactionException() {
        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        tx.commit(pool);

        try {
            tx.abort(pool);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertCommitted(tx);
    }
}
