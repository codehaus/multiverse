package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.programmatic.AlphaProgrammaticLongRef;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.stms.alpha.transactions.SpeculativeConfiguration;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
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
        stmConfig.maxRetries = 10;
        stm = new AlphaStm(stmConfig);
    }

    public AlphaTransaction createSutTransaction(int size) {
        UpdateConfiguration config = new UpdateConfiguration(stmConfig.clock);
        return new ArrayUpdateAlphaTransaction(config, size);
    }

    public AlphaTransaction createSutTransaction(int size, int maximumSize) {
        SpeculativeConfiguration speculativeConfig = new SpeculativeConfiguration(maximumSize);
        UpdateConfiguration config = new UpdateConfiguration(stmConfig.clock)
                .withSpeculativeConfiguration(speculativeConfig);

        return new ArrayUpdateAlphaTransaction(config, size);
    }

    @Test
    public void whenTransactionalObjectLocked() {
        AlphaProgrammaticLongRef ref = new AlphaProgrammaticLongRef(stm, 10);

        AlphaTransaction tx = createSutTransaction(10);

        Transaction lockOwner = mock(Transaction.class);
        ref.___tryLock(lockOwner);

        AlphaTranlocal loaded = tx.openForCommutingWrite(ref);
        assertTrue(loaded.isCommuting());
    }

    @Test
    public void whenTransactionalObjectAlreadyOpenedForConstruction() {
        AlphaProgrammaticLongRef ref = AlphaProgrammaticLongRef.createUncommitted(stm);

        AlphaTransaction tx = createSutTransaction(10);
        AlphaTranlocal openedForConstruction = tx.openForConstruction(ref);
        AlphaTranlocal found = tx.openForCommutingWrite(ref);

        assertSame(openedForConstruction, found);
        assertFalse(found.isCommuting());
        assertFalse(found.isCommitted());
    }

    @Test
    public void whenTransactionalObjectAlreadyOpenedForWrite() {
        AlphaProgrammaticLongRef ref = new AlphaProgrammaticLongRef(stm, 1);

        AlphaTransaction tx = createSutTransaction(10);
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
        AlphaProgrammaticLongRef ref = new AlphaProgrammaticLongRef(stm, 1);

        AlphaTransaction tx = createSutTransaction(10);
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
        AlphaProgrammaticLongRef ref = new AlphaProgrammaticLongRef(stm, 1);

        AlphaTransaction tx = createSutTransaction(10);
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

        AlphaTransaction tx = createSutTransaction(10);
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

        AlphaTransaction tx = createSutTransaction(10);
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

        AlphaTransaction tx = createSutTransaction(10);
        tx.prepare();

        try {
            tx.openForCommutingWrite(ref);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsPrepared(tx);
    }
}
