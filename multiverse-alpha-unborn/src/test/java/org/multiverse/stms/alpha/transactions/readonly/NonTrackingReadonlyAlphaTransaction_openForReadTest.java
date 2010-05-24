package org.multiverse.stms.alpha.transactions.readonly;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.*;
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
        stmConfig.maxRetries = 10;
        stm = new AlphaStm(stmConfig);
    }

    public NonTrackingReadonlyAlphaTransaction createSutTransaction() {
        ReadonlyConfiguration config = new ReadonlyConfiguration(stmConfig.clock, false)
                .withMaxRetries(10);
        return new NonTrackingReadonlyAlphaTransaction(config);
    }

    @Test
    public void whenOpenedForRead_thenNotLockTxObjects() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = createSutTransaction();
        ref.resetLockInfo();
        tx.openForRead(ref);

        ref.assertNoLocksReleased();
        ref.assertNoLockAcquired();
    }

    @Test
    public void openedForReadWithNull_thenNullReturned() {
        AlphaTransaction tx = createSutTransaction();
        AlphaTranlocal result = tx.openForRead(null);
        assertNull(result);
    }

    @Test
    public void whenNotCommittedBefore_thenUncommittedReadConflict() {
        ManualRef ref = ManualRef.createUncommitted();

        AlphaTransaction tx = createSutTransaction();
        try {
            tx.openForRead(ref);
            fail();
        } catch (UncommittedReadConflict expected) {
        }

        assertIsActive(tx);
    }

    @Test
    public void whenVersionExactMatch() {
        ManualRef ref = new ManualRef(stm, 10);

        ManualRefTranlocal expected = (ManualRefTranlocal) ref.___load(stm.getVersion());

        AlphaTransaction tx = createSutTransaction();
        ManualRefTranlocal found = (ManualRefTranlocal) tx.openForRead(ref);
        assertTrue(found.isCommitted());
        assertSame(expected, found);
    }

    @Test
    public void whenVersionIsOlder() {
        ManualRef ref = new ManualRef(stm, 10);
        AlphaTranlocal committed = ref.___load();

        stmConfig.clock.tick();

        AlphaTransaction tx = createSutTransaction();
        AlphaTranlocal tranlocal = tx.openForRead(ref);
        assertSame(committed, tranlocal);
    }

    @Test
    public void whenLockedButExactVersionMatch_thenSuccess() {
        ManualRef ref = new ManualRef(stm);
        AlphaTranlocal readonly = ref.___load();

        AlphaTransaction owner = mock(AlphaTransaction.class);
        ref.___tryLock(owner);

        AlphaTransaction tx = createSutTransaction();

        AlphaTranlocal tranlocal = tx.openForRead(ref);

        assertIsActive(tx);
        assertSame(readonly, tranlocal);
    }

    @Test
    public void whenLockedAndVersionTooNew_thenOldVersionNotFoundReadConflict() {
        ManualRef ref = new ManualRef(stm, 1);

        //start the transaction to sets its readversion
        AlphaTransaction tx = createSutTransaction();
        tx.start();

        //do an atomic and conflicting update
        ref.set(stm, 10);

        ManualRefTranlocal expectedTranlocal = (ManualRefTranlocal) ref.___load();

        //lock it
        Transaction owner = mock(Transaction.class);
        ref.___tryLock(owner);

        //try to load it, it should fail because the version stored is newer than the
        //readversion is the transaction allows.
        long version = stm.getVersion();
        try {
            tx.openForRead(ref);
            fail();
        } catch (OldVersionNotFoundReadConflict ex) {
        }

        assertIsActive(tx);
        assertEquals(version, stm.getVersion());
        assertEquals(expectedTranlocal, ref.___load());
    }

    @Test
    public void whenLockedAndVersionTooOld_thenLockNotFreeReadConflict() {
        ManualRef ref = new ManualRef(stm, 1);

        //lock it
        Transaction owner = mock(Transaction.class);
        ref.___tryLock(owner);


        stm.getClock().tick();

        //start the transaction to sets its readversion
        AlphaTransaction tx = createSutTransaction();

        ManualRefTranlocal expectedTranlocal = (ManualRefTranlocal) ref.___load();

        //try to load it, it should fail because the version stored is newer than the
        //readversion is the transaction allows.
        long version = stm.getVersion();
        try {
            tx.openForRead(ref);
            fail();
        } catch (LockNotFreeReadConflict ex) {
        }

        assertIsActive(tx);
        assertEquals(version, stm.getVersion());
        assertEquals(expectedTranlocal, ref.___load());
    }

    @Test
    public void whenOtherTransactionHasPendingWrite_noChangesAreSeen() {
        ManualRef ref = new ManualRef(stm, 0);

        AlphaTransaction readonlyTx = createSutTransaction();
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

        AlphaTransaction readonlyTx = createSutTransaction();
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
        } catch (OldVersionNotFoundReadConflict ex) {
        }
    }

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        ManualRef value = new ManualRef(stm, 10);

        AlphaTransaction tx = createSutTransaction();
        tx.prepare();

        long version = stm.getVersion();
        try {
            tx.openForRead(value);
            fail();
        } catch (PreparedTransactionException ex) {
        }

        assertIsPrepared(tx);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        ManualRef value = new ManualRef(stm, 10);

        AlphaTransaction tx = createSutTransaction();
        tx.commit();

        long version = stm.getVersion();
        try {
            tx.openForRead(value);
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsCommitted(tx);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        ManualRef value = new ManualRef(stm, 10);

        AlphaTransaction tx = createSutTransaction();
        tx.abort();

        long version = stm.getVersion();
        try {
            tx.openForRead(value);
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsAborted(tx);
        assertEquals(version, stm.getVersion());
    }
}
