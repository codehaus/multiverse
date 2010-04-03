package org.multiverse.stms.alpha.transactions.readonly;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.*;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRefTranlocal;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.stms.alpha.transactions.SpeculativeConfiguration;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.multiverse.TestUtils.*;

public class ArrayReadonlyAlphaTransaction_openForReadTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
    }

    public ArrayReadonlyAlphaTransaction startTransactionUnderTest() {
        return startTransactionUnderTest(new SpeculativeConfiguration(100));
    }

    public ArrayReadonlyAlphaTransaction startTransactionUnderTest(SpeculativeConfiguration speculativeConfig) {
        ReadonlyConfiguration config = new ReadonlyConfiguration(stmConfig.clock)
                .withSpeculativeConfig(speculativeConfig);
        return new ArrayReadonlyAlphaTransaction(config, speculativeConfig.getMaximumArraySize());
    }

    @Test
    public void whenTxObjectNull_thenNullReturned() {
        AlphaTransaction tx = startTransactionUnderTest();
        AlphaTranlocal tranlocal = tx.openForRead(null);
        assertNull(tranlocal);
    }

    @Test
    public void whenNotCommittedBefore_thenUncommittedReadConflict() {
        ManualRef ref = ManualRef.createUncommitted();

        AlphaTransaction tx = startTransactionUnderTest();
        try {
            tx.openForRead(ref);
            fail();
        } catch (UncommittedReadConflict expected) {
        }

        assertIsActive(tx);
    }

    @Test
    public void whenFirstTimeOpenForRead_thenReadTracked() {
        ManualRef ref = new ManualRef(stm);
        ManualRefTranlocal committed = (ManualRefTranlocal) ref.___load();

        AlphaTransaction tx = startTransactionUnderTest();
        ManualRefTranlocal found = (ManualRefTranlocal) tx.openForRead(ref);

        assertTrue(found.isCommitted());
        assertFalse(found.isCommuting());
        assertSame(committed, found);
        assertEquals(1, getField(tx, "firstFreeIndex"));
    }

    @Test
    public void whenSecondTimeOpenedForRead_thenNotAddedToAttachedSet() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startTransactionUnderTest();
        ManualRefTranlocal expected = (ManualRefTranlocal) tx.openForRead(ref);
        ManualRefTranlocal found = (ManualRefTranlocal) tx.openForRead(ref);

        assertTrue(found.isCommitted());
        assertFalse(found.isCommuting());
        assertSame(expected, found);
        assertEquals(1, getField(tx, "firstFreeIndex"));
    }

    @Test
    public void whenDifferentOpenForReadsAreDone() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);
        ManualRef ref3 = new ManualRef(stm);

        AlphaTransaction tx = startTransactionUnderTest();
        tx.openForRead(ref1);
        assertEquals(1, getField(tx, "firstFreeIndex"));

        tx.openForRead(ref2);
        assertEquals(2, getField(tx, "firstFreeIndex"));

        tx.openForRead(ref3);
        assertEquals(3, getField(tx, "firstFreeIndex"));
    }

    @Test
    public void whenLocked_thenLockNotFreeReadConflict() {
        ManualRef ref = new ManualRef(stm);
        AlphaTransaction owner = mock(AlphaTransaction.class);
        ref.___tryLock(owner);

        AlphaTransaction tx = startTransactionUnderTest();

        try {
            tx.openForRead(ref);
            fail();
        } catch (LockNotFreeReadConflict expected) {
        }

        assertIsActive(tx);
        assertEquals(0, getField(tx, "firstFreeIndex"));
    }

    @Test
    public void whenVersionTooNew_thenOldVersionNotFoundReadConflict() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startTransactionUnderTest();

        //conflicting write
        ref.inc(stm);

        try {
            tx.openForRead(ref);
            fail();
        } catch (OldVersionNotFoundReadConflict expected) {
        }

        assertIsActive(tx);
        assertEquals(0, getField(tx, "firstFreeIndex"));
    }

    @Test
    public void whenMaximumCapacityExceeded_thenSpeculativeConfigurationFailure() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);
        ManualRef ref3 = new ManualRef(stm);
        ManualRef ref4 = new ManualRef(stm);

        SpeculativeConfiguration speculativeConfig = new SpeculativeConfiguration(3);
        AbstractReadonlyAlphaTransaction tx = startTransactionUnderTest(speculativeConfig);
        tx.openForRead(ref1);
        tx.openForRead(ref2);
        tx.openForRead(ref3);

        try {
            tx.openForRead(ref4);
            fail();
        } catch (SpeculativeConfigurationFailure expected) {
        }

        assertEquals(5, speculativeConfig.getOptimalSize());
    }

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startTransactionUnderTest();
        tx.prepare();

        long version = stm.getVersion();
        try {
            tx.openForRead(ref);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsPrepared(tx);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startTransactionUnderTest();
        tx.commit();

        long version = stm.getVersion();
        try {
            tx.openForRead(ref);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startTransactionUnderTest();
        tx.abort();

        long version = stm.getVersion();
        try {
            tx.openForRead(ref);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertEquals(version, stm.getVersion());
    }
}
