package org.multiverse.stms.alpha.transactions.readonly;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.LoadLockedException;
import org.multiverse.api.exceptions.LoadTooOldVersionException;
import org.multiverse.api.exceptions.LoadUncommittedException;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRefTranlocal;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.alpha.AlphaTestUtils.startTrackingUpdateTransaction;

/**
 * @author Peter Veentjer
 */
public class NonTrackingReadonlyAlphaTransaction_openForReadTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
    }

    public NonTrackingReadonlyAlphaTransaction startSutTransaction() {
        NonTrackingReadonlyAlphaTransaction.Config config = new NonTrackingReadonlyAlphaTransaction.Config(
                stmConfig.clock,
                stmConfig.backoffPolicy,
                null,
                stmConfig.profiler,
                stmConfig.maxRetryCount);
        return new NonTrackingReadonlyAlphaTransaction(config);
    }

    public AlphaTransaction startUpdateTransaction() {
        return startTrackingUpdateTransaction(stm);
    }

    @Test
    public void whenOpenedForRead_thenNotLockTxObjects() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        ref.resetLockInfo();
        tx.openForRead(ref);

        assertFalse(ref.isTryLockCalled());
    }

    @Test
    public void openedForReadWithNull_thenNullReturned() {
        AlphaTransaction tx = startSutTransaction();
        AlphaTranlocal result = tx.openForRead(null);
        assertNull(result);
    }

    @Test
    public void whenNotCommittedBefore_thenLoadUncommittedException() {
        ManualRef ref = ManualRef.createUncommitted();

        AlphaTransaction tx = startSutTransaction();
        try {
            tx.openForRead(ref);
            fail();
        } catch (LoadUncommittedException expected) {
        }

        assertIsActive(tx);
    }

    @Test
    public void whenVersionExactMatch() {
        ManualRef ref = new ManualRef(stm, 10);

        ManualRefTranlocal expected = (ManualRefTranlocal) ref.___load(stm.getVersion());

        AlphaTransaction tx = startSutTransaction();
        ManualRefTranlocal found = (ManualRefTranlocal) tx.openForRead(ref);
        assertTrue(found.isCommitted());
        assertSame(expected, found);
    }

    @Test
    public void whenVersionIsOlder() {
        ManualRef ref = new ManualRef(stm, 10);
        AlphaTranlocal committed = ref.___load();

        stmConfig.clock.tick();

        AlphaTransaction tx = startSutTransaction();
        AlphaTranlocal tranlocal = tx.openForRead(ref);
        assertSame(committed, tranlocal);
    }

    @Test
    public void whenLocked() {
        ManualRef ref = new ManualRef(stm);

        Transaction owner = mock(Transaction.class);
        ref.___tryLock(owner);

        AlphaTransaction tx = startSutTransaction();

        try {
            tx.openForRead(ref);
            fail();
        } catch (LoadLockedException expected) {
        }

        assertIsActive(tx);
    }

    @Test
    public void whenOtherTransactionHasPendingWrite_noChangesAreSeen() {
        ManualRef ref = new ManualRef(stm, 0);

        AlphaTransaction readonlyTx = startSutTransaction();
        AlphaTransaction updateTx = startTrackingUpdateTransaction(stm);
        ManualRefTranlocal tranlocal = (ManualRefTranlocal) updateTx.openForWrite(ref);
        tranlocal.value++;

        ManualRefTranlocal read = (ManualRefTranlocal) readonlyTx.openForRead(ref);
        assertEquals(0, read.value);
    }

    /**
     * Since readonly transactions does not track reads (see the {@linkplain NonTrackingReadonlyAlphaTransaction
     * JavaDoc}), it will immediately see a <em>committed</em> change made by another transaction.
     * <p/>
     * If read tracking is implemented this behaviour is expected to change, i.e. loads after commits by other
     * transactions should still succeed and return the value that was current when the readonly transaction started.
     */
    @Test
    public void whenVersionTooOld_thenLoadTooOldVersionException() {
        ManualRef ref = new ManualRef(stm, 0);

        AlphaTransaction readonlyTx = startSutTransaction();
        AlphaTransaction updateTx = startTrackingUpdateTransaction(stm);
        ManualRefTranlocal tranlocal = (ManualRefTranlocal) updateTx.openForWrite(ref);
        tranlocal.value++;

        // will succeed because the updating transaction hasn't committed yet
        ManualRefTranlocal found = (ManualRefTranlocal) readonlyTx.openForRead(ref);
        assertEquals(0, found.value);
        updateTx.commit();

        // will fail because the version requested is too old (no read tracking)
        try {
            readonlyTx.openForRead(ref);
            fail();
        } catch (LoadTooOldVersionException ex) {
        }
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        ManualRef value = new ManualRef(stm, 10);

        AlphaTransaction tx = startSutTransaction();
        tx.commit();

        try {
            tx.openForRead(value);
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsCommitted(tx);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        ManualRef value = new ManualRef(stm, 10);

        AlphaTransaction tx = startSutTransaction();
        tx.abort();

        try {
            tx.openForRead(value);
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsAborted(tx);
    }
}
