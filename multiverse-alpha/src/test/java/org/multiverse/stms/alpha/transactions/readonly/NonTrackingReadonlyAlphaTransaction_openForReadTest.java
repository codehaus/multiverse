package org.multiverse.stms.alpha.transactions.readonly;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.LoadTooOldVersionException;
import org.multiverse.api.exceptions.LoadUncommittedException;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRefTranlocal;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.*;
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

    public NonTrackingReadonlyAlphaTransaction startTransactionUnderTest() {
        NonTrackingReadonlyAlphaTransaction.Config config = new NonTrackingReadonlyAlphaTransaction.Config(
                stmConfig.clock,
                stmConfig.restartBackoffPolicy,
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

        AlphaTransaction tx = startTransactionUnderTest();
        ref.resetLockInfo();
        tx.openForRead(ref);

        assertFalse(ref.isTryLockCalled());
    }

    @Test
    public void openedForReadWithNull_thenNullReturned() {
        AlphaTransaction tx = startTransactionUnderTest();
        AlphaTranlocal result = tx.openForRead(null);
        assertNull(result);
    }

    @Test
    public void whenUncommitted_loadUncommittedException() {
        ManualRef ref = ManualRef.createUncommitted();

        AlphaTransaction tx = startTransactionUnderTest();

        try {
            tx.openForRead(ref);
            fail();
        } catch (LoadUncommittedException ex) {
        }

        assertIsActive(tx);
    }

    @Test
    public void whenVersionExactMatch() {
        ManualRef ref = new ManualRef(stm, 10);

        ManualRefTranlocal expected = (ManualRefTranlocal) ref.___load(stm.getVersion());

        AlphaTransaction tx = startTransactionUnderTest();
        ManualRefTranlocal found = (ManualRefTranlocal) tx.openForRead(ref);
        assertTrue(found.isCommitted());
        assertSame(expected, found);
    }

    @Test
    public void whenVersionIsOlder() {
        ManualRef ref = new ManualRef(stm, 10);
        AlphaTranlocal committed = ref.___load();

        stmConfig.clock.tick();

        AlphaTransaction tx = startTransactionUnderTest();
        AlphaTranlocal tranlocal = tx.openForRead(ref);
        assertSame(committed, tranlocal);
    }

    @Test
    public void whenLocked() {
        testIncomplete();
    }

    @Test
    public void whenOtherTransactionHasPendingWrite_noChangesAreSeen() {
        ManualRef ref = new ManualRef(stm, 0);

        AlphaTransaction readonlyTx = startTransactionUnderTest();
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

        AlphaTransaction readonlyTx = startTransactionUnderTest();
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

        AlphaTransaction tx = startTransactionUnderTest();
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

        AlphaTransaction tx = startTransactionUnderTest();
        tx.abort();

        try {
            tx.openForRead(value);
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsAborted(tx);
    }
}
