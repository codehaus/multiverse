package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
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
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.StmUtils.createLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertUnlocked;

/**
 * @author Peter Veentjer
 */
public class ArrayBetaTransaction_registerRetryListenerTest {
    private BetaStm stm;
    private ObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new ObjectPool();
    }

    @Test
    @Ignore
    public void whenUnstarted(){

    }

    @Test
    public void whenMultipleItems_thenRegisteredMultipleTimes(){
        LongRef ref1 = createLongRef(stm);
        LongRef ref2 = createLongRef(stm);
        LongRef ref3 = createLongRef(stm);

        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 3);
        tx.openForRead(ref1, false, pool);
        tx.openForRead(ref2, false, pool);
        tx.openForRead(ref3, false, pool);
        Latch latch = new CheapLatch();
        tx.registerChangeListenerAndAbort(latch,pool);

        assertHasListeners(ref1, latch);
        assertHasListeners(ref2, latch);
        assertHasListeners(ref3, latch);
    }

    @Test
    public void whenNoReads_thenNoRetryPossibleException() {
        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 1);
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

        ArrayBetaTransaction tx = new ArrayBetaTransaction(config, 1);
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

        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 1);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        Latch latch = new CheapLatch();
        tx.registerChangeListenerAndAbort(latch, pool);

        assertFalse(latch.isOpen());
        assertAborted(tx);
    }

    @Test
    public void whenLockedRead_thenSuccessAndLockReleased() {
        LongRef ref = createLongRef(stm);

        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 1);
        LongRefTranlocal read = tx.openForRead(ref, true, pool);

        Latch latch = new CheapLatch();
        tx.registerChangeListenerAndAbort(latch, pool);

        assertFalse(latch.isOpen());
        assertAborted(tx);
        assertNull(ref.getLockOwner());
        assertUnlocked(ref);
    }

    @Test
    public void whenContainsWrite_thenSuccess() {
        LongRef ref = createLongRef(stm);

        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 1);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);

        Latch latch = new CheapLatch();
        tx.registerChangeListenerAndAbort(latch, pool);

        assertFalse(latch.isOpen());
        assertAborted(tx);
    }

      @Test
    public void whenLockedWrite_thenSuccessAndLockReleased() {
        LongRef ref = createLongRef(stm);

        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 1);
        LongRefTranlocal write = tx.openForWrite(ref, true, pool);

        Latch latch = new CheapLatch();
        tx.registerChangeListenerAndAbort(latch, pool);

        assertFalse(latch.isOpen());
        assertAborted(tx);
        assertNull(ref.getLockOwner());
        assertUnlocked(ref);
    }

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        Latch latch = mock(Latch.class);
        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 1);
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
        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 1);
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
        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 1);
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
