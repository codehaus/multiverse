package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.*;
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
import static org.multiverse.stms.alpha.transactions.AlphaTransactionTestUtils.assertUpdatableClone;

public class ArrayUpdateAlphaTransaction_openForWriteTest {

    private AlphaStmConfig stmConfig;
    private AlphaStm stm;
    private OptimalSize optimalSize;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
        optimalSize = new OptimalSize(1);
    }

    public AlphaTransaction startSutTransaction(int size) {
        optimalSize.set(size);
        ArrayUpdateAlphaTransaction.Config config = new ArrayUpdateAlphaTransaction.Config(
                stmConfig.clock,
                stmConfig.backoffPolicy,
                null,
                stmConfig.profiler,
                stmConfig.commitLockPolicy,
                stmConfig.maxRetryCount,
                true,
                optimalSize,
                true, true, true, true, size
        );
        return new ArrayUpdateAlphaTransaction(config, size);
    }

    @Test
    public void whenFirstTime() {
        ManualRef txObject = ManualRef.createUncommitted();

        AlphaTransaction tx = startSutTransaction(2);
        AlphaTranlocal tranlocal = tx.openForWrite(txObject);

        assertNotNull(tranlocal);
        assertSame(txObject, tranlocal.getTransactionalObject());
        assertTrue(tranlocal.isUncommitted());
        assertNull(txObject.___getLockOwner());
    }

    @Test
    public void whenFull() {
        ManualRef txObject1 = ManualRef.createUncommitted();
        ManualRef txObject2 = ManualRef.createUncommitted();

        AlphaTransaction tx = startSutTransaction(1);
        tx.openForWrite(txObject1);

        try {
            tx.openForWrite(txObject2);
            fail();
        } catch (TransactionTooSmallError ex) {
        }

        assertIsActive(tx);
        assertEquals(3, optimalSize.get());
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
        assertUpdatableClone(ref, committed, found);
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
        assertUpdatableClone(ref, committed, found);
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
    public void whenMaximumCapacityExceeded_thenTransactionTooSmallError() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);
        ManualRef ref3 = new ManualRef(stm);
        ManualRef ref4 = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction(3);
        tx.openForWrite(ref1);
        tx.openForWrite(ref2);
        tx.openForWrite(ref3);

        try {
            tx.openForWrite(ref4);
            fail();
        } catch (TransactionTooSmallError expected) {
        }

        assertEquals(5, optimalSize.get());
        assertIsActive(tx);
    }


    @Test
    public void whenLocked_thenLockNotFreeReadConflict() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction owner = mock(AlphaTransaction.class);
        ref.___tryLock(owner);

        AlphaTransaction tx = startSutTransaction(10);
        try {
            tx.openForWrite(ref);
            fail();
        } catch (LockNotFreeReadConflict expected) {
        }

        assertIsActive(tx);
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
