package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.stms.alpha.AlphaProgrammaticLong;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.stms.alpha.transactions.SpeculativeConfiguration;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;

/**
 * @author Peter Veentjer
 */
public class ArrayUpdateAlphaTransaction_openForCommutingWriteTest {

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
    @Ignore
    public void test() {
    }

    @Test
    public void whenTransactionalObjectAlreadyOpenedForWrite() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 1);

        AlphaTransaction tx = startSutTransaction(10);
        AlphaTranlocal openedForWrite = tx.openForWrite(ref);
        AlphaTranlocal found = tx.openForCommutingWrite(ref);

        //assertSame(found, getField(tx, "attached"));
        assertSame(openedForWrite, found);
        assertFalse(found.isCommitted());
        assertFalse(found.isCommuting());
        assertEquals(0, found.getWriteVersion());
    }

    @Test
    public void whenTransactionalObjectAlreadyOpenedForRead() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 1);

        AlphaTransaction tx = startSutTransaction(10);
        AlphaTranlocal openedForRead = tx.openForRead(ref);
        AlphaTranlocal found = tx.openForCommutingWrite(ref);

        //assertSame(found, getField(tx, "attached"));
        assertNotNull(found);
        assertFalse(openedForRead == found);
        assertSame(openedForRead, found.getOrigin());
        assertFalse(found.isCommitted());
        assertFalse(found.isCommuting());
        assertEquals(0, found.getWriteVersion());
    }

    @Test
    public void whenTransactionalObjectAlreadyOpenedForCommutingWrite() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 1);

        AlphaTransaction tx = startSutTransaction(10);
        AlphaTranlocal firstOpen = tx.openForCommutingWrite(ref);
        AlphaTranlocal found = tx.openForCommutingWrite(ref);

        //assertSame(found, getField(tx, "attached"));
        assertSame(found, firstOpen);
        assertFalse(found.isCommitted());
        assertTrue(found.isCommuting());
        assertEquals(-2, found.getWriteVersion());
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction(10);
        tx.commit();

        try {
            tx.openForCommutingWrite(ref);
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsCommitted(tx);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction(10);
        tx.abort();

        try {
            tx.openForCommutingWrite(ref);
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction(10);
        tx.prepare();

        try {
            tx.openForCommutingWrite(ref);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsPrepared(tx);
    }
}
