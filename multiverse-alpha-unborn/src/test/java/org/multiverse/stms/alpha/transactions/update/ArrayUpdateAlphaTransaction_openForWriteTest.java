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

public class ArrayUpdateAlphaTransaction_openForWriteTest {

    private AlphaStmConfig stmConfig;
    private AlphaStm stm;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
    }

    public AlphaTransaction startSutTransaction(int size) {
        UpdateConfiguration config =
                new UpdateConfiguration(stmConfig.clock);
        return new ArrayUpdateAlphaTransaction(config, size);
    }

    public AlphaTransaction startSutTransaction(int size, int maximumSize) {
        SpeculativeConfiguration speculativeConfig = new SpeculativeConfiguration(maximumSize);

        UpdateConfiguration config = new UpdateConfiguration(stmConfig.clock)
                .withSpeculativeConfiguration(speculativeConfig);

        return new ArrayUpdateAlphaTransaction(config, size);
    }

    @Test
    public void whenOpeningUncommittedObject_thenUncommittedReadConflict() {
        ManualRef txObject = ManualRef.createUncommitted();

        AlphaTransaction tx = startSutTransaction(2);

        long version = stm.getVersion();
        try {
            tx.openForWrite(txObject);
            fail();
        } catch (UncommittedReadConflict expected) {

        }

        assertNull(txObject.___load());
        assertNull(txObject.___getLockOwner());
        assertEquals(version, stm.getVersion());
        assertIsActive(tx);
    }

    @Test
    public void whenFull() {
        ManualRef txObject1 = new ManualRef(stm, 1);
        ManualRef txObject2 = new ManualRef(stm, 1);

        AlphaTransaction tx = startSutTransaction(1, 1);
        tx.openForWrite(txObject1);

        try {
            tx.openForWrite(txObject2);
            fail();
        } catch (SpeculativeConfigurationFailure ex) {
        }

        assertIsActive(tx);
        //todo
        //assertEquals(3, speculativeConfig.get());
    }

    @Test
    public void whenUpdate() {
        ManualRef txObject = new ManualRef(stm, 10);
        ManualRefTranlocal original = (ManualRefTranlocal) txObject.___load();

        AlphaTransaction tx = startSutTransaction(1);

        long version = stm.getVersion();
        ManualRefTranlocal tranlocal = (ManualRefTranlocal) tx.openForWrite(txObject);

        assertEquals(version, stm.getVersion());
        assertNotNull(tranlocal);
        assertTrue(tranlocal.isUncommitted());
        assertSame(txObject, tranlocal.getTransactionalObject());
        assertSame(original, tranlocal.getOrigin());
        assertEquals(10, tranlocal.value);
    }

    @Test
    public void whenNullTxObject_thenNullPointerException() {
        AlphaTransaction tx = startSutTransaction(1);

        try {
            tx.openForWrite(null);
            fail();
        } catch (NullPointerException expected) {

        }
        assertIsActive(tx);
    }

    @Test
    public void whenNotLoadedBefore_updatableVersionReturned() {
        ManualRef ref = new ManualRef(stm);
        ManualRefTranlocal committed = (ManualRefTranlocal) ref.___load();

        AlphaTransaction tx = startSutTransaction(10);
        ManualRefTranlocal found = (ManualRefTranlocal) tx.openForWrite(ref);

        assertIsActive(tx);
        assertIsUpdatableClone(ref, committed, found);
    }

    @Test
    public void whenAlreadyOpenedForRead_updatableVersionReturned() {
        ManualRef ref = new ManualRef(stm);
        ManualRefTranlocal committed = (ManualRefTranlocal) ref.___load();

        AlphaTransaction tx = startSutTransaction(10);
        tx.openForRead(ref);
        ManualRefTranlocal found = (ManualRefTranlocal) tx.openForWrite(ref);

        assertEquals(1, getField(tx, "firstFreeIndex"));
        assertIsActive(tx);
        assertIsUpdatableClone(ref, committed, found);
    }

    @Test
    public void whenAlreadyOpenedForWrite_sameVersionReturned() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction(10);
        ManualRefTranlocal expected = (ManualRefTranlocal) tx.openForWrite(ref);
        ManualRefTranlocal found = (ManualRefTranlocal) tx.openForWrite(ref);

        assertSame(expected, found);
        assertEquals(1, getField(tx, "firstFreeIndex"));
        assertIsActive(tx);
    }

    @Test
    public void whenAlreadyOpenedForCommutingWrite_thenFixated() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 0);

        AlphaTransaction tx = startSutTransaction(10);
        AlphaTranlocal openedForCommutingWrite = tx.openForCommutingWrite(ref);
        AlphaTranlocal found = tx.openForWrite(ref);

        assertSame(openedForCommutingWrite, found);
        assertFalse(found.isCommuting());
        assertFalse(found.isCommitted());
    }

    @Test
    public void whenAlreadyOpenedForCommutingWriteAndLockedButVersionMatches() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 0);

        AlphaTransaction tx = startSutTransaction(10);
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

        AlphaTransaction tx = startSutTransaction(10);
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
    public void whenMaximumCapacityExceeded_thenTransactionTooSmallError() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);
        ManualRef ref3 = new ManualRef(stm);
        ManualRef ref4 = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction(3, 3);
        tx.openForWrite(ref1);
        tx.openForWrite(ref2);
        tx.openForWrite(ref3);

        try {
            tx.openForWrite(ref4);
            fail();
        } catch (SpeculativeConfigurationFailure expected) {
        }

        //todo
        //assertEquals(5, speculativeConfig.get());
        assertIsActive(tx);
    }

    @Test
    public void whenLockedButExactVersionMatch_thenSuccess() {
        ManualRef ref = new ManualRef(stm);
        AlphaTranlocal readonly = ref.___load();

        AlphaTransaction owner = mock(AlphaTransaction.class);
        ref.___tryLock(owner);

        AlphaTransaction tx = startSutTransaction(100);

        AlphaTranlocal tranlocal = tx.openForWrite(ref);

        assertIsActive(tx);
        assertSame(readonly, tranlocal.getOrigin());
        assertFalse(tranlocal.isCommitted());
    }

    @Test
    public void whenLockedAndVersionTooOld_thenOldVersionNotFoundReadConflict() {
        ManualRef ref = new ManualRef(stm, 1);

        //start the transaction to sets its readversion
        AlphaTransaction tx = startSutTransaction(100);

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

        AlphaTransaction tx = startSutTransaction(10);
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
    public void whenAlreadyOpenedForRead_thenNotSubjectToReadConflict() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction(10);
        tx.openForRead(ref);
        //conflicting write
        ref.inc(stm);

        tx.openForWrite(ref);
        assertTrue(true);
    }

    @Test
    public void whenAlreadyOpenedForWrite_thenNotSubjectToReadConflict() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction(10);
        tx.openForWrite(ref);
        //conflicting write
        ref.inc(stm);

        tx.openForWrite(ref);
        assertTrue(true);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction(1);
        tx.abort();

        try {
            tx.openForWrite(ref);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction(1);
        tx.commit();

        try {
            tx.openForWrite(ref);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
    }

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction(1);
        tx.prepare();

        try {
            tx.openForWrite(ref);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsPrepared(tx);
    }
}
