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
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class MonoBetaTransaction_registerChangeListenerAndAbortTest {

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
    public void whenNoReads_thenNoRetryPossibleException() {
        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
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

        MonoBetaTransaction tx = new MonoBetaTransaction(config);
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

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        Latch latch = new CheapLatch();
        tx.registerChangeListenerAndAbort(latch, pool);

        assertFalse(latch.isOpen());
        assertUnlocked(ref);
        assertNull(ref.getLockOwner());
        assertSurplus(0, ref);
        assertAborted(tx);
    }

    @Test
    public void whenLockedRead_thenSuccessAndLockReleased() {
        LongRef ref = createLongRef(stm);

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, true, pool);

        Latch latch = new CheapLatch();
        tx.registerChangeListenerAndAbort(latch, pool);

        assertFalse(latch.isOpen());
        assertSurplus(0, ref);
        assertAborted(tx);
        assertNull(ref.getLockOwner());
        assertUnlocked(ref);
    }

    @Test
    public void whenContainsWrite_thenSuccess() {
        LongRef ref = createLongRef(stm);

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);

        Latch latch = new CheapLatch();
        tx.registerChangeListenerAndAbort(latch, pool);

        assertFalse(latch.isOpen());
        assertSurplus(0, ref);
        assertAborted(tx);
    }

    @Test
    public void whenLockedWrite_thenSuccessAndLockReleased() {
        LongRef ref = createLongRef(stm);

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, true, pool);

        Latch latch = new CheapLatch();
        tx.registerChangeListenerAndAbort(latch, pool);

        assertFalse(latch.isOpen());
        assertSurplus(0, ref);
        assertAborted(tx);
        assertNull(ref.getLockOwner());
        assertUnlocked(ref);
    }

    @Test
    public void whenContainsConstructed() {
        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        LongRef ref = new LongRef(tx);
        LongRefTranlocal constructed = tx.openForConstruction(ref, pool);

        tx.prepare(pool);

        assertPrepared(tx);
        assertLocked(ref);
        assertSame(tx, ref.getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertNull(ref.unsafeLoad());
        assertNull(constructed.read);
        assertSame(ref, constructed.owner);
        assertFalse(constructed.isCommitted);
        assertFalse(constructed.isPermanent);

        fail();
    }

    @Test
    public void whenAlreadyPrepared_thenPreparedTransactionException() {
        Latch latch = mock(Latch.class);
        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
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
    public void whenAlreadyCommitted_thenDeadTransactionException() {
        Latch latch = mock(Latch.class);
        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
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
    public void whenAlreadyAborted_thenDeadTransactionException() {
        Latch latch = mock(Latch.class);
        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
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
