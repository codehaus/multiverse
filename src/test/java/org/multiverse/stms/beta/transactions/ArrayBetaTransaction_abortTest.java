package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
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
public class ArrayBetaTransaction_abortTest {
    private BetaStm stm;
    private ObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new ObjectPool();
    }

    @Test
    public void whenUnused() {
        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        tx.abort(pool);

        assertAborted(tx);
    }

    @Test
    public void whenHasUnlockedWrite() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();
        int oldReadonlyCount = ref.getReadonlyCount();

        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        tx.openForWrite(ref, false, pool);

        tx.abort(pool);

        assertAborted(tx);
        assertUnlocked(ref);
        assertSame(committed, ref.unsafeLoad());
        assertSurplus(0, ref);
        assertNull(ref.getLockOwner());
        assertReadonlyCount(oldReadonlyCount, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenHasLockedWrite() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();
        int oldReadonlyCount = ref.getReadonlyCount();

        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        tx.openForWrite(ref, true, pool);

        tx.abort(pool);

        assertAborted(tx);
        assertUnlocked(ref);
        assertSame(committed, ref.unsafeLoad());
        assertSurplus(0, ref);
        assertNull(ref.getLockOwner());
        assertReadonlyCount(oldReadonlyCount, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenHasLockedRead() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();
        int oldReadonlyCount = ref.getReadonlyCount();

        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        tx.openForRead(ref, true, pool);

        tx.abort(pool);

        assertAborted(tx);
        assertUnlocked(ref);
        assertSame(committed, ref.unsafeLoad());
        assertSurplus(0, ref);
        assertNull(ref.getLockOwner());
        assertReadonlyCount(oldReadonlyCount, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenHasPermanentReadThatIsNotLocked() {
        LongRef ref = createReadBiasedLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();
        int oldReadonlyCount = ref.getReadonlyCount();

        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        tx.openForRead(ref, false, pool);

        tx.abort(pool);

        assertAborted(tx);
        assertUnlocked(ref);
        assertReadBiased(ref);
        assertReadonlyCount(oldReadonlyCount, ref);
        assertSurplus(1, ref);
        assertSame(committed, ref.unsafeLoad());
        assertNull(ref.getLockOwner());
    }

    @Test
    public void whenHasConstructed() {
        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 1);
        LongRef ref = new LongRef(tx);
        LongRefTranlocal write = tx.openForConstruction(ref, pool);
        tx.abort(pool);

        assertAborted(tx);

        assertSame(tx, ref.getLockOwner());
        assertLocked(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertNull(ref.unsafeLoad());
        assertFalse(write.isPermanent);
        assertFalse(write.isCommitted);
    }

    @Test
    @Ignore
    public void whenHasReads() {

    }

    @Test
    @Ignore
    public void whenPrepared() {

    }

    @Test
    public void whenAborted_thenIgnored() {
        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        tx.abort(pool);

        tx.abort(pool);
        assertAborted(tx);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        tx.commit(pool);

        try {
            tx.abort(pool);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertCommitted(tx);
    }
}
