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
import org.multiverse.stms.alpha.programmatic.AlphaProgrammaticLong;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.multiverse.TestUtils.*;

/**
 * @author Peter Veentjer
 */
public class MapUpdateAlphaTransaction_openForCommutingWriteTest {
    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stmConfig.maxRetries = 10;
        stm = new AlphaStm(stmConfig);
    }

    public MapUpdateAlphaTransaction startSutTransaction() {
        UpdateConfiguration config =
                new UpdateConfiguration(stmConfig.clock);
        return new MapUpdateAlphaTransaction(config);
    }

    @Test
    public void whenTransactionalObjectLocked() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 10);

        AlphaTransaction tx = startSutTransaction();

        Transaction lockOwner = mock(Transaction.class);
        ref.___tryLock(lockOwner);

        AlphaTranlocal loaded = tx.openForCommutingWrite(ref);
        assertTrue(loaded.isCommuting());
    }

    @Test
    public void whenTransactionalObjectAlreadyOpenedForConstruction() {
        AlphaProgrammaticLong ref = AlphaProgrammaticLong.createUncommitted(stm);

        AlphaTransaction tx = startSutTransaction();
        AlphaTranlocal openedForConstruction = tx.openForConstruction(ref);
        AlphaTranlocal found = tx.openForCommutingWrite(ref);

        assertSame(openedForConstruction, found);
        assertFalse(found.isCommuting());
        assertFalse(found.isCommitted());
    }

    @Test
    public void whenTransactionalObjectAlreadyOpenedForWrite() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 1);

        AlphaTransaction tx = startSutTransaction();
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

        AlphaTransaction tx = startSutTransaction();
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

        AlphaTransaction tx = startSutTransaction();
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

        AlphaTransaction tx = startSutTransaction();
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

        AlphaTransaction tx = startSutTransaction();
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

        AlphaTransaction tx = startSutTransaction();
        tx.prepare();

        try {
            tx.openForCommutingWrite(ref);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsPrepared(tx);
    }
}
