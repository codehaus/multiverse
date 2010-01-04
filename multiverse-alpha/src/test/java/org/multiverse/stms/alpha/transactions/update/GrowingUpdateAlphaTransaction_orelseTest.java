package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRefTranlocal;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class GrowingUpdateAlphaTransaction_orelseTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
    }

    public GrowingUpdateAlphaTransaction startSutTransaction() {
        GrowingUpdateAlphaTransaction.Config config = new GrowingUpdateAlphaTransaction.Config(
                stmConfig.clock,
                stmConfig.restartBackoffPolicy,
                null,
                stmConfig.profiler,
                stmConfig.commitLockPolicy,
                stmConfig.maxRetryCount,
                false, true,true,true,true);
        return new GrowingUpdateAlphaTransaction(config);
    }

    @Test
    public void commitWithOutstandingOr() {
        ManualRef ref1 = new ManualRef(stm, 0);
        ManualRef ref2 = new ManualRef(stm, 0);

        AlphaTransaction tx = startSutTransaction();
        ref1.inc(tx);
        tx.startOr();
        ref2.inc(tx);

        tx.commit();
        assertIsCommitted(tx);

        assertEquals(1, ref1.get(stm));
        assertEquals(1, ref2.get(stm));
    }

    @Test
    public void abortWithOutstandingOr() {
        ManualRef ref1 = new ManualRef(stm, 0);
        ManualRef ref2 = new ManualRef(stm, 0);

        AlphaTransaction tx = startSutTransaction();
        ref1.inc(tx);
        tx.startOr();
        ref2.inc(tx);

        tx.abort();
        assertIsAborted(tx);

        assertEquals(0, ref1.get(stm));
        assertEquals(0, ref2.get(stm));
    }

    @Test
    public void rollbackScenario() {
        ManualRef ref = new ManualRef(stm, 0);

        AlphaTransaction tx = startSutTransaction();
        ManualRefTranlocal tranlocal = (ManualRefTranlocal) tx.openForWrite(ref);

        //start the or, and do a check that the tranlocal has not changed.
        tx.startOr();
        assertSame(tranlocal, tx.openForWrite(ref));

        //now do an write that is going to be rolled back.
        tranlocal.value++;

        //now do a rollback.
        tx.endOrAndStartElse();

        //check that the inc has been rolled back.
        assertEquals(0, ref.get(stm));
    }

    @Test
    public void rollbackScenarioWithMultipleLevels() {
        ManualRef ref = new ManualRef(stm, 0);

        AlphaTransaction tx = startSutTransaction();
        ref.inc(tx);
        tx.startOr();
        ref.inc(tx);
        tx.startOr();
        ref.inc(tx);

        assertEquals(3, ref.get(tx));
        tx.endOrAndStartElse();
        assertEquals(2, ref.get(tx));
        tx.endOrAndStartElse();
        assertEquals(1, ref.get(tx));
        tx.commit();

        assertEquals(1, ref.get(stm));
    }

    @Test
    public void multipleLevelsOfEndOr() {
        ManualRef ref = new ManualRef(stm, 0);

        long startVersion = stm.getVersion();
        AlphaTransaction tx = startSutTransaction();
        ref.inc(tx);
        tx.startOr();
        assertEquals(1, ref.get(tx));
        ref.inc(tx);

        tx.startOr();
        assertEquals(2, ref.get(tx));

        tx.endOr();
        assertEquals(2, ref.get(tx));

        tx.endOr();
        assertEquals(2, ref.get(tx));

        tx.commit();

        assertEquals(startVersion + 1, stm.getVersion());
        setThreadLocalTransaction(null);
        assertEquals(2, ref.get(stm));
    }

    @Test
    public void scenarioWithNothingToCommitAfterARollback() {
        ManualRef ref = new ManualRef(stm, 0);

        long startVersion = stm.getVersion();
        AlphaTransaction tx = startSutTransaction();
        tx.startOr();
        ref.inc(tx);
        tx.endOrAndStartElse();
        tx.commit();

        setThreadLocalTransaction(null);

        assertEquals(startVersion, stm.getVersion());
        assertIsCommitted(tx);
        assertEquals(0, ref.get(stm));
    }

    @Test
    public void endOrFailsIfCalledTooEarly() {
        Transaction tx = startSutTransaction();

        try {
            tx.endOr();
            fail();
        } catch (IllegalStateException ex) {
        }

        assertIsActive(tx);
    }

    @Test
    public void endOrAndStartElseIfCalledTooEarly() {
        Transaction tx = startSutTransaction();

        try {
            tx.endOrAndStartElse();
            fail();
        } catch (IllegalStateException ex) {
        }

        assertIsActive(tx);
    }

    @Test
    public void emptyStartOrEndOrDoesNotFail() {
        long startVersion = stm.getVersion();

        Transaction tx = startSutTransaction();
        tx.startOr();
        tx.endOr();
        tx.commit();

        assertIsCommitted(tx);
        assertEquals(startVersion, stm.getVersion());
    }

    @Test
    public void emptyStartOrEndOrAndStartElseDoesNotFail() {
        long startVersion = stm.getVersion();

        Transaction tx = startSutTransaction();
        tx.startOr();
        tx.endOrAndStartElse();
        tx.commit();

        assertIsCommitted(tx);
        assertEquals(startVersion, stm.getVersion());
    }

    @Test
    public void startOrFailsIfTransactionAlreadyIsAborted() {
        Transaction tx = startSutTransaction();
        tx.abort();

        try {
            tx.startOr();
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void endOrFailsIfTransactionAlreadyIsAborted() {
        Transaction tx = startSutTransaction();
        tx.abort();

        try {
            tx.endOr();
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void endOrAndStartElseFailsIfTransactionAlreadyIsAborted() {
        Transaction tx = startSutTransaction();
        tx.abort();

        try {
            tx.endOrAndStartElse();
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void startOr_whenCommitted_thenDeadTransactionException() {
        Transaction tx = startSutTransaction();
        tx.commit();

        try {
            tx.startOr();
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsCommitted(tx);
    }

    @Test
    public void endOr_whenCommitted_thenDeadTransaction() {
        Transaction tx = startSutTransaction();
        tx.commit();

        try {
            tx.endOr();
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsCommitted(tx);
    }

    @Test
    public void endOrAndStartElse_whenAborted_thenDeadTransaction() {
        Transaction tx = startSutTransaction();
        tx.commit();

        try {
            tx.endOrAndStartElse();
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsCommitted(tx);
    }
}
