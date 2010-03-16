package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.LockNotFreeReadConflict;
import org.multiverse.api.exceptions.OldVersionNotFoundReadConflict;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRefTranlocal;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.stms.alpha.transactions.OptimalSize;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.multiverse.TestUtils.*;

public class MonoUpdateAlphaTransaction_openForReadTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;
    private OptimalSize optimalSize;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
        optimalSize = new OptimalSize(1);
    }

    public MonoUpdateAlphaTransaction startSutTransaction() {
        MonoUpdateAlphaTransaction.Config config = new MonoUpdateAlphaTransaction.Config(
                stmConfig.clock,
                stmConfig.backoffPolicy,
                null,
                stmConfig.maxRetryCount,
                stmConfig.commitLockPolicy, true, optimalSize, true, true, true, true);
        return new MonoUpdateAlphaTransaction(config);
    }

    public MonoUpdateAlphaTransaction startSutTransactionWithoutAutomaticReadTracking() {
        MonoUpdateAlphaTransaction.Config config = new MonoUpdateAlphaTransaction.Config(
                stmConfig.clock,
                stmConfig.backoffPolicy,
                null,
                stmConfig.maxRetryCount,
                stmConfig.commitLockPolicy, true, optimalSize, true, true, true, false);
        return new MonoUpdateAlphaTransaction(config);
    }

    @Test
    public void testAutomaticReadTrackingProperty() {
        AlphaTransaction tx = startSutTransactionWithoutAutomaticReadTracking();
        assertFalse(tx.getConfig().automaticReadTracking());

        tx = startSutTransaction();
        assertTrue(tx.getConfig().automaticReadTracking());
    }

    @Test
    public void whenAutomaticReadTrackingDisabled_openForReadIsNotTracked() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransactionWithoutAutomaticReadTracking();
        tx.openForRead(ref);

        AlphaTranlocal tranlocal = (AlphaTranlocal) getField(tx, "attached");
        assertNull("attached field was not null", getField(tx, "attached"));
    }

    @Test
    public void whenTxObjectNull_thenNullReturned() {
        AlphaTransaction tx = startSutTransaction();

        AlphaTranlocal tranlocal = tx.openForRead(null);

        assertNull(tranlocal);
        assertIsActive(tx);
    }

    @Test
    public void whenNotCommittedBefore_thenFreshTranlocalReturned() {
        ManualRef ref = ManualRef.createUncommitted();

        AlphaTransaction tx = startSutTransaction();
        AlphaTranlocal found = tx.openForRead(ref);

        assertNotNull(found);
        assertSame(ref, found.getTransactionalObject());
        assertTrue(found.isUncommitted());
    }

    @Test
    public void whenLocked_thenLockNotFreeReadConflict() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction lockOwner = mock(AlphaTransaction.class);
        ref.___tryLock(lockOwner);

        long version = stm.getVersion();
        AlphaTransaction tx = startSutTransaction();
        try {
            tx.openForRead(ref);
            fail();
        } catch (LockNotFreeReadConflict expected) {
        }

        assertIsActive(tx);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenOlderVersionExists_thenOlderVersionReturned() {
        ManualRef ref = new ManualRef(stm);
        AlphaTranlocal committed = ref.___load();
        stmConfig.clock.tick();
        AlphaTransaction tx = startSutTransaction();

        ManualRefTranlocal found = (ManualRefTranlocal) tx.openForRead(ref);
        assertSame(committed, found);
        assertSame(committed, getField(tx, "attached"));
    }

    @Test
    public void whenVersionMatch() {
        ManualRef ref = new ManualRef(stm);
        ManualRefTranlocal committed = (ManualRefTranlocal) ref.___load();
        long version = stm.getVersion();
        AlphaTransaction tx = startSutTransaction();

        ManualRefTranlocal tranlocal = (ManualRefTranlocal) tx.openForRead(ref);

        assertSame(committed, tranlocal);
        assertSame(committed, getField(tx, "attached"));
        assertIsActive(tx);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenVersionTooNew_thenOldVersionNotFoundReadConflict() {
        ManualRef ref = new ManualRef(stm);
        AlphaTransaction tx = startSutTransaction();
        //the 'conflicting' update
        ref.inc(stm);

        try {
            tx.openForWrite(ref);
            fail();
        } catch (OldVersionNotFoundReadConflict expected) {
        }

        assertIsActive(tx);
        assertNull(getField(tx, "attached"));
    }

    @Test
    public void whenOpenForReadWhenAlreadyOpenedForWrite_thenSameTranlocalReturned() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        AlphaTranlocal tranlocal = tx.openForWrite(ref);
        AlphaTranlocal found = tx.openForRead(ref);

        assertSame(tranlocal, found);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        tx.commit();

        try {
            tx.openForRead(ref);
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsCommitted(tx);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        tx.abort();

        try {
            tx.openForRead(ref);
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        tx.prepare();

        try {
            tx.openForRead(ref);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsPrepared(tx);
    }
}
