package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.ObjectPool;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.multiverse.TestUtils.assertActive;
import static org.multiverse.TestUtils.getField;

/**
 * @author Peter Veentjer
 */
public class ArrayTreeBetaTransaction_resetTest {
    private BetaStm stm;
    private ObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new ObjectPool();
    }

    @Test
    @Ignore
    public void test() {
    }

    @Test
    @Ignore
    public void whenPrepared() {

    }

    @Test
    public void whenHasPermanentListener() {
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.registerPermanent(listener);

        tx.reset(pool);

        verifyZeroInteractions(listener);
        assertNull(getField(tx, "normalListeners"));
        assertSame(listener, getField(tx, "permanentListeners"));
    }

    @Test
    public void whenHasNormalListener() {
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.register(listener);

        tx.reset(pool);

        verifyZeroInteractions(listener);
        assertNull(getField(tx, "normalListeners"));
        assertNull(getField(tx, "permanentListeners"));
    }

    @Test
    public void whenAborted() {
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        tx.abort(pool);

        tx.reset(pool);
        assertActive(tx);
    }

    @Test
    public void whenCommitted() {
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        tx.commit(pool);

        tx.reset(pool);
        assertActive(tx);
    }
}
