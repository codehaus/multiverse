package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.SpeculativeConfigurationFailure;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRefTranlocal;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.stms.alpha.transactions.SpeculativeConfiguration;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;

/**
 * @author Peter Veentjer
 */
public class ArrayUpdateAlphaTransaction_openForConstructionTest {

    private AlphaStmConfig stmConfig;
    private AlphaStm stm;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
    }

    public AlphaTransaction startSutTransaction(int size) {
        SpeculativeConfiguration speculativeConfig = new SpeculativeConfiguration(100);
        UpdateAlphaTransactionConfiguration config = new UpdateAlphaTransactionConfiguration(
                stmConfig.clock,
                stmConfig.backoffPolicy,
                stmConfig.commitLockPolicy,
                null,
                speculativeConfig,
                stmConfig.maxRetryCount, true, true, true, true, true);

        return new ArrayUpdateAlphaTransaction(config, size);
    }

    public AlphaTransaction startSutTransaction(int size, int maximumSize) {
        SpeculativeConfiguration speculativeConfig = new SpeculativeConfiguration(maximumSize);
        UpdateAlphaTransactionConfiguration config = new UpdateAlphaTransactionConfiguration(
                stmConfig.clock,
                stmConfig.backoffPolicy,
                stmConfig.commitLockPolicy,
                null,
                speculativeConfig,
                stmConfig.maxRetryCount, true, true, true, true, true);

        return new ArrayUpdateAlphaTransaction(config, size);
    }

    @Test
    public void whenNullTxObject_thenNullPointerException() {
        AlphaTransaction tx = startSutTransaction(1);

        long version = stm.getVersion();
        try {
            tx.openForConstruction(null);
            fail();
        } catch (NullPointerException expected) {

        }

        assertIsActive(tx);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenCalledForTheFirstTime() {
        ManualRef ref = ManualRef.createUncommitted();

        AlphaTransaction tx = startSutTransaction(1);

        long version = stm.getVersion();
        ManualRefTranlocal tranlocal = (ManualRefTranlocal) tx.openForConstruction(ref);

        assertEquals(version, stm.getVersion());
        assertNotNull(tranlocal);
        assertTrue(tranlocal.isUncommitted());
        assertSame(ref, tranlocal.getTransactionalObject());
        assertNull(tranlocal.getOrigin());
        assertEquals(0, tranlocal.value);
    }

    @Test
    public void whenObjectNotInConstruction() {

    }

    @Test
    public void whenAlreadyOpenedForConstruction() {
        ManualRef ref = ManualRef.createUncommitted();

        AlphaTransaction tx = startSutTransaction(1);

        long version = stm.getVersion();
        ManualRefTranlocal firstTime = (ManualRefTranlocal) tx.openForConstruction(ref);
        ManualRefTranlocal found = (ManualRefTranlocal) tx.openForConstruction(ref);

        assertEquals(version, stm.getVersion());
        assertSame(firstTime, found);
        assertTrue(found.isUncommitted());
        assertSame(ref, found.getTransactionalObject());
        assertNull(found.getOrigin());
        assertEquals(0, found.value);
    }

    @Test
    @Ignore
    public void whenAlreadyOpenedForRead() {

    }

    @Test
    @Ignore
    public void whenAlreadyOpenedForWrite() {

    }

    @Test
    @Ignore
    public void whenAlreadyOpenedForCommutingWrite() {

    }

    @Test
    public void whenMaximumCapacityExceeded_thenTransactionTooSmallError() {
        ManualRef ref1 = ManualRef.createUncommitted();
        ManualRef ref2 = ManualRef.createUncommitted();
        ManualRef ref3 = ManualRef.createUncommitted();
        ManualRef ref4 = ManualRef.createUncommitted();

        AlphaTransaction tx = startSutTransaction(3, 3);
        tx.openForConstruction(ref1);
        tx.openForConstruction(ref2);
        tx.openForConstruction(ref3);

        long version = stm.getVersion();
        try {
            tx.openForConstruction(ref4);
            fail();
        } catch (SpeculativeConfigurationFailure expected) {
        }

        //todo
        //assertEquals(5, speculativeConfig.get());
        assertIsActive(tx);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        ManualRef ref = ManualRef.createUncommitted();

        AlphaTransaction tx = startSutTransaction(1);
        tx.abort();

        long version = stm.getVersion();
        try {
            tx.openForConstruction(ref);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        ManualRef ref = ManualRef.createUncommitted();

        AlphaTransaction tx = startSutTransaction(1);
        tx.commit();

        long version = stm.getVersion();
        try {
            tx.openForConstruction(ref);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        ManualRef ref = ManualRef.createUncommitted();

        AlphaTransaction tx = startSutTransaction(1);
        tx.prepare();

        long version = stm.getVersion();
        try {
            tx.openForConstruction(ref);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsPrepared(tx);
        assertEquals(version, stm.getVersion());
    }
}
