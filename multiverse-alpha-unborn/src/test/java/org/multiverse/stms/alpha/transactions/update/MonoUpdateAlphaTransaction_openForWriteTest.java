package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.*;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRefTranlocal;
import org.multiverse.stms.alpha.programmatic.AlphaProgrammaticLong;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.stms.alpha.transactions.SpeculativeConfiguration;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.alpha.transactions.AlphaTransactionTestUtils.assertIsUpdatableClone;

public class MonoUpdateAlphaTransaction_openForWriteTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stmConfig.maxRetries = 10;
        stm = new AlphaStm(stmConfig);
    }

    public MonoUpdateAlphaTransaction createSutTransaction(SpeculativeConfiguration speculativeConfig) {
        UpdateConfiguration config = new UpdateConfiguration(stmConfig.clock)
                .withSpeculativeConfiguration(speculativeConfig);
        return new MonoUpdateAlphaTransaction(config);
    }

    public MonoUpdateAlphaTransaction createSutTransaction() {
        return createSutTransaction(new SpeculativeConfiguration(100));
    }

    @Test
    public void whenNullTxObject_thenNullPointerException() {
        AlphaTransaction tx = createSutTransaction();
        tx.start();

        try {
            tx.openForWrite(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertIsActive(tx);
    }

    @Test
    public void whenOpenForWriteFirstTime_thenAttached() {
        ManualRef ref = new ManualRef(stm);
        AlphaTranlocal committed = ref.___load();
        AlphaTransaction tx = createSutTransaction();

        AlphaTranlocal found = tx.openForWrite(ref);

        assertIsUpdatableClone(ref, committed, found);
        assertSame(found, getField(tx, "attached"));
    }

    @Test
    public void whenOpenForWriteOnFreshObject_thenUncommittedReadConflict() {
        ManualRef ref = ManualRef.createUncommitted();
        AlphaTransaction tx = createSutTransaction();

        long version = stm.getVersion();
        try {
            tx.openForWrite(ref);
            fail();
        } catch (UncommittedReadConflict expected) {

        }

        assertNull(ref.___load());
        assertNull(getField(tx, "attached"));
        assertEquals(version, stm.getVersion());
        assertIsActive(tx);
    }

    @Test
    public void whenVersionTooNew_thenOldVersionNotFoundReadConflict() {
        ManualRef ref = new ManualRef(stm);
        AlphaTransaction tx = createSutTransaction();
        tx.start();

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
    public void whenLockedAndVersionTooOld_thenOldVersionNotFoundReadConflict() {
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
            tx.openForWrite(ref);
            fail();
        } catch (OldVersionNotFoundReadConflict ex) {
        }

        assertIsActive(tx);
        assertEquals(version, stm.getVersion());
        assertEquals(expectedTranlocal, ref.___load());
    }

    @Test
    public void whenReadConflict_thenOldVersionNotFoundReadConflict() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = createSutTransaction();
        tx.start();

        //conflicting write
        ref.inc(stm);
        try {
            tx.openForWrite(ref);
            fail();
        } catch (OldVersionNotFoundReadConflict expected) {
        }

        assertIsActive(tx);
    }

    @Test
    public void whenOpenForWriteSecondTime_thenPreviousTranlocalReturned() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = createSutTransaction();
        AlphaTranlocal found1 = tx.openForWrite(ref);
        AlphaTranlocal found2 = tx.openForWrite(ref);
        assertSame(found1, found2);
    }

    @Test
    public void whenAlreadyOpenedForRead_thenUpgradedToOpenedForWrite() {
        ManualRef ref = new ManualRef(stm);
        AlphaTranlocal committed = ref.___load();

        AlphaTransaction tx = createSutTransaction();
        tx.openForRead(ref);
        AlphaTranlocal found = tx.openForWrite(ref);

        assertIsUpdatableClone(ref, committed, found);
        assertSame(found, getField(tx, "attached"));
    }

    @Test
    public void whenAlreadyOpenedForCommutingWrite() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 0);

        AlphaTransaction tx = createSutTransaction();
        AlphaTranlocal openedForCommuting = tx.openForCommutingWrite(ref);
        AlphaTranlocal found = tx.openForWrite(ref);

        assertIsActive(tx);
        assertSame(openedForCommuting, found);
        assertFalse(found.isCommuting());
        assertFalse(found.isCommitted());
    }

    @Test
    public void whenAlreadyOpenedForCommutingWriteAndLockedButVersionMatches() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 0);

        AlphaTransaction tx = createSutTransaction();
        AlphaTranlocal openedForCommutingWrite = tx.openForCommutingWrite(ref);

        Transaction lockOwner = mock(Transaction.class);
        ref.___tryLock(lockOwner);

        AlphaTranlocal found = tx.openForWrite(ref);

        assertSame(openedForCommutingWrite, found);
        assertFalse(found.isCommuting());
        assertFalse(found.isCommitted());
    }

    @Test
    public void whenAlreadyOpenedForCommutingWriteAndLockedAndVersionTooOld_thenOldVersionNotFoundReadConflict() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 0);

        AlphaTransaction tx = createSutTransaction();
        AlphaTranlocal openedForCommutingWrite = tx.openForCommutingWrite(ref);

        ref.atomicInc(10);

        Transaction lockOwner = mock(Transaction.class);
        ref.___tryLock(lockOwner);

        long version = stm.getVersion();
        try {
            tx.openForWrite(ref);
            fail();
        } catch (OldVersionNotFoundReadConflict expected) {
        }

        assertEquals(stm.getVersion(), version);
        assertIsActive(tx);
        assertTrue(openedForCommutingWrite.isCommuting());
    }

    @Test
    public void whenAlreadyAnotherOpenForRead_thenSpeculativeConfigurationFailure() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);

        SpeculativeConfiguration speculativeConfig = new SpeculativeConfiguration(100);
        AlphaTransaction tx = createSutTransaction(speculativeConfig);
        tx.openForRead(ref1);

        try {
            tx.openForWrite(ref2);
            fail();
        } catch (SpeculativeConfigurationFailure ex) {
        }

        assertIsActive(tx);
        assertEquals(2, speculativeConfig.getOptimalSize());
    }


    @Test
    public void whenAlreadyAnotherOpenForWrite_thenSpeculativeConfigurationFailure() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);

        SpeculativeConfiguration speculativeConfig = new SpeculativeConfiguration(100);
        AlphaTransaction tx = createSutTransaction(speculativeConfig);
        tx.openForWrite(ref1);

        try {
            tx.openForWrite(ref2);
            fail();
        } catch (SpeculativeConfigurationFailure ex) {
        }

        assertIsActive(tx);
        assertEquals(2, speculativeConfig.getOptimalSize());
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        ManualRef ref = new ManualRef(stm);
        ManualRefTranlocal committed = (ManualRefTranlocal) ref.___load();

        AlphaTransaction tx = createSutTransaction();
        tx.abort();

        long version = stm.getVersion();

        try {
            tx.openForWrite(ref);
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsAborted(tx);
        assertEquals(version, stm.getVersion());
        assertSame(committed, ref.___load());
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        ManualRef ref = new ManualRef(stm);
        ManualRefTranlocal committed = (ManualRefTranlocal) ref.___load();

        AlphaTransaction tx = createSutTransaction();
        tx.commit();

        long version = stm.getVersion();

        try {
            tx.openForWrite(ref);
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsCommitted(tx);
        assertEquals(version, stm.getVersion());
        assertSame(committed, ref.___load());
    }

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = createSutTransaction();
        tx.prepare();

        try {
            tx.openForWrite(ref);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsPrepared(tx);
    }
}

