package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.ObjectPool;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.LongRefTranlocal;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.multiverse.stms.beta.StmUtils.createLongRef;
import static org.multiverse.TestUtils.assertActive;
import static org.multiverse.TestUtils.getField;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class ArrayBetaTransaction_resetTest {
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
        tx.reset();

        assertActive(tx);
    }

    @Test
    public void whenAttemptUsedThenReset() {
        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        tx.incAttempt();

        tx.reset();
        //assertEquals(1, tx.getAttempt());
    }

    @Test
    public void whenContainsNormalUpdate() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        tx.reset(pool);

        assertActive(tx);
        assertUnlocked(ref);
        assertNull(ref.getLockOwner());
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertSame(committed, ref.unsafeLoad());
        assertTrue(committed.isCommitted);
        assertFalse(write.isCommitted);
    }

    @Test
    public void whenContainsLockedUpdate() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        LongRefTranlocal write = tx.openForWrite(ref, true, pool);
        tx.reset(pool);

        assertActive(tx);
        assertUnlocked(ref);
        assertNull(ref.getLockOwner());
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertSame(committed, ref.unsafeLoad());
        assertTrue(committed.isCommitted);
        assertFalse(write.isCommitted);
    }

    @Test
    public void whenHasPermanentListener() {
        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.registerPermanent(listener);

        tx.reset(pool);

        verifyZeroInteractions(listener);
        assertNull(getField(tx, "normalListeners"));
        assertSame(listener, getField(tx, "permanentListeners"));
    }

    @Test
    public void whenHasNormalListener() {
        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.register(listener);

        tx.reset(pool);

        verifyZeroInteractions(listener);
        assertNull(getField(tx, "normalListeners"));
        assertNull(getField(tx, "permanentListeners"));
    }

    @Test
    @Ignore
    public void whenContainsConstructed(){
        
    }

    @Test
    public void whenPreparedAndUnused() {
        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        tx.prepare(pool);

        tx.reset();
        assertActive(tx);
    }

    @Test
    @Ignore
    public void whenPreparedResourcesNeedRelease() {

    }

    @Test
    public void whenAborted() {
        BetaTransaction tx = new ArrayBetaTransaction(stm, 1);
        tx.abort(pool);

        tx.reset(pool);
        assertActive(tx);
    }

    @Test
    public void whenCommitted() {
        BetaTransaction tx = new ArrayBetaTransaction(stm, 1);
        tx.commit(pool);

        tx.reset(pool);
        assertActive(tx);
    }
}
