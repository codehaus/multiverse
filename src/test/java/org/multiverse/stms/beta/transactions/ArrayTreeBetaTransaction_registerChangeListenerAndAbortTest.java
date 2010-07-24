package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.blocking.CheapLatch;
import org.multiverse.api.blocking.Latch;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.NoRetryPossibleException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.ObjectPool;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.LongRefTranlocal;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.multiverse.stms.beta.StmUtils.createLongRef;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertSurplus;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertUnlocked;

/**
 * @author Peter Veentjer
 */
public class ArrayTreeBetaTransaction_registerChangeListenerAndAbortTest {
    private BetaStm stm;
    private ObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new ObjectPool();
    }

    @Test
    public void whenMultipleReads_thenMultipleRegisters() {
        LongRef ref1 = createLongRef(stm);
        LongRef ref2 = createLongRef(stm);
        LongRef ref3 = createLongRef(stm);

        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        tx.openForRead(ref1, false, pool);
        tx.openForRead(ref2, false, pool);
        tx.openForRead(ref3, false, pool);

        Latch latch = new CheapLatch();
        tx.registerChangeListenerAndAbort(latch, pool);

        assertAborted(tx);
        assertFalse(latch.isOpen());
        assertHasListeners(ref1, latch);
        assertHasListeners(ref2, latch);
        assertHasListeners(ref3, latch);
    }

    @Test
    public void whenNoReads_thenNoRetryPossibleException() {
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        Latch latch = new CheapLatch();

        try {
            tx.registerChangeListenerAndAbort(latch, pool);
            fail();
        } catch (NoRetryPossibleException expected) {
        }

        assertActive(tx);
    }

    @Test
    public void whenExplicitRetryNotAllowed_thenNoRetryPossibleException() {
        LongRef ref = createLongRef(stm);

        BetaTransactionConfig config = new BetaTransactionConfig(stm)
                .setExplicitRetryAllowed(false);

        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(config);
        tx.openForRead(ref, false, pool);

        Latch latch = new CheapLatch();

        try {
            tx.registerChangeListenerAndAbort(latch, pool);
            fail();
        } catch (NoRetryPossibleException expected) {
        }

        assertActive(tx);
    }

    @Test
    public void whenContainsRead_thenSuccess() {
        LongRef ref = createLongRef(stm);

        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        Latch latch = new CheapLatch();
        tx.registerChangeListenerAndAbort(latch, pool);

        assertFalse(latch.isOpen());
        assertAborted(tx);
        assertSurplus(0,ref);
        assertUnlocked(ref);
    }

    @Test
    public void whenLockedRead_thenSuccessAndLockReleased() {
        LongRef ref = createLongRef(stm);

        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, true, pool);

        Latch latch = new CheapLatch();
        tx.registerChangeListenerAndAbort(latch, pool);

        assertFalse(latch.isOpen());
        assertAborted(tx);
        assertNull(ref.getLockOwner());
        assertUnlocked(ref);
        assertSurplus(0,ref);
    }

    @Test
    public void whenContainsWrite_thenSuccess() {
        LongRef ref = createLongRef(stm);

        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);

        Latch latch = new CheapLatch();
        tx.registerChangeListenerAndAbort(latch, pool);

        assertFalse(latch.isOpen());
        assertAborted(tx);
        assertSurplus(0,ref);
        assertNull(ref.getLockOwner());
        assertUnlocked(ref);
    }

    @Test
    public void whenLockedWrite_thenSuccessAndLockReleased() {
        LongRef ref = createLongRef(stm);

        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, true, pool);

        Latch latch = new CheapLatch();
        tx.registerChangeListenerAndAbort(latch, pool);

        assertFalse(latch.isOpen());
        assertAborted(tx);
        assertNull(ref.getLockOwner());
        assertSurplus(0,ref);
        assertUnlocked(ref);
    }

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        Latch latch = mock(Latch.class);
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        tx.prepare(pool);

        try {
            tx.registerChangeListenerAndAbort(latch, pool);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertPrepared(tx);
        verifyZeroInteractions(latch);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        Latch latch = mock(Latch.class);
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        tx.commit(pool);

        try {
            tx.registerChangeListenerAndAbort(latch, pool);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertCommitted(tx);
        verifyZeroInteractions(latch);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        Latch latch = mock(Latch.class);
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        tx.abort(pool);

        try {
            tx.registerChangeListenerAndAbort(latch, pool);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertAborted(tx);
        verifyZeroInteractions(latch);
    }
}
