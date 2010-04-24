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
import org.multiverse.stms.alpha.transactions.SpeculativeConfiguration;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.multiverse.TestUtils.*;

public class MonoReadonlyAlphaTransaction_openForReadTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createFastConfig();
        stm = new AlphaStm(stmConfig);
    }

    public MonoReadonlyAlphaTransaction startSutTransaction() {
        return startSutTransaction(new SpeculativeConfiguration(true, true, true, 100));
    }

    public MonoReadonlyAlphaTransaction startSutTransaction(SpeculativeConfiguration speculativeConfig) {
        ReadonlyConfiguration config = new ReadonlyConfiguration(stmConfig.clock, true)
                .withSpeculativeConfig(speculativeConfig);
        return new MonoReadonlyAlphaTransaction(config);
    }

    @Test
    public void whenSuccess() {
        ManualRef ref = new ManualRef(stm);
        ManualRefTranlocal committed = (ManualRefTranlocal) ref.___load();

        AlphaTransaction tx = startSutTransaction();
        ManualRefTranlocal found = (ManualRefTranlocal) tx.openForRead(ref);

        assertTrue(found.isCommitted());
        assertFalse(found.isCommuting());
        assertSame(committed, found);
        assertSame(committed, getField(tx, "attached"));
    }

    @Test
    public void whenNotCommittedBefore_thenUncommittedReadConflict() {
        ManualRef ref = ManualRef.createUncommitted();

        AlphaTransaction tx = startSutTransaction();
        try {
            tx.openForRead(ref);
            fail();
        } catch (UncommittedReadConflict expected) {
        }

        assertIsActive(tx);
    }

    @Test
    public void whenLockedButExactVersionMatch_thenSuccess() {
        ManualRef ref = new ManualRef(stm);
        AlphaTranlocal readonly = ref.___load();

        AlphaTransaction owner = mock(AlphaTransaction.class);
        ref.___tryLock(owner);

        AlphaTransaction tx = startSutTransaction();

        AlphaTranlocal tranlocal = tx.openForRead(ref);

        assertIsActive(tx);
        assertSame(readonly, tranlocal);
    }

    @Test
    public void whenLockedAndVersionTooNew_thenOldVersionNotFoundReadConflict() {
        ManualRef ref = new ManualRef(stm, 1);

        //start the transaction to sets its readversion
        AlphaTransaction tx = startSutTransaction();

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
        AlphaTransaction tx = startSutTransaction();

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
    public void whenAlreadyAttached_thenSameTranlocalReturned() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        ManualRefTranlocal found1 = (ManualRefTranlocal) tx.openForRead(ref);
        ManualRefTranlocal found2 = (ManualRefTranlocal) tx.openForRead(ref);

        assertTrue(found2.isCommitted());
        assertFalse(found2.isCommuting());
        assertSame(found1, found2);
    }

    @Test
    public void whenReadConflict_thenOldVersionNotFoundReadConflict() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        ref.inc(stm);
        try {
            tx.openForRead(ref);
            fail();
        } catch (OldVersionNotFoundReadConflict expected) {
        }

        assertIsActive(tx);
        assertNull(getField(tx, "attached"));
    }

    @Test
    public void whenLoaded_thenNotSubjectToReadConflict() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        AlphaTranlocal tranlocal1 = tx.openForRead(ref);
        ref.inc(stm);
        AlphaTranlocal tranlocal2 = tx.openForRead(ref);
        assertSame(tranlocal1, tranlocal2);
    }

    @Test
    public void whenMaximumCapacityIsReached_thenTransactionTooSmallException() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);

        SpeculativeConfiguration speculativeConfig = new SpeculativeConfiguration(true, true, true, 100);
        AlphaTransaction tx = startSutTransaction(speculativeConfig);
        tx.openForRead(ref1);

        try {
            tx.openForRead(ref2);
            fail();
        } catch (SpeculativeConfigurationFailure expected) {
        }

        assertIsActive(tx);
        assertEquals(2, speculativeConfig.getOptimalSize());
    }

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
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
    public void whenAborted_thenDeadTransactionException() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
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

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
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
}
