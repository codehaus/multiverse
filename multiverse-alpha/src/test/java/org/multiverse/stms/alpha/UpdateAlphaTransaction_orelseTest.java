package org.multiverse.stms.alpha;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.stms.alpha.manualinstrumentation.IntRef;
import org.multiverse.stms.alpha.manualinstrumentation.IntRefTranlocal;

public class UpdateAlphaTransaction_orelseTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = new AlphaStm();
        setGlobalStmInstance(stm);
        setThreadLocalTransaction(null);
    }

    public AlphaTransaction startUpdateTransaction() {
        AlphaTransaction t = stm.startUpdateTransaction(null);
        setThreadLocalTransaction(t);
        return t;
    }

    @Test
    public void commitWithOutstandingOr() {
        IntRef v1 = new IntRef(0);
        IntRef v2 = new IntRef(0);

        Transaction t = startUpdateTransaction();
        v1.inc();
        t.startOr();
        v2.inc();

        t.commit();
        assertIsCommitted(t);
        setThreadLocalTransaction(null);

        assertEquals(1, v1.get());
        assertEquals(1, v2.get());
    }

    @Test
    public void abortWithOutstandingOr() {
        IntRef v1 = new IntRef(0);
        IntRef v2 = new IntRef(0);

        Transaction t = startUpdateTransaction();
        v1.inc();
        t.startOr();
        v2.inc();

        t.abort();
        assertIsAborted(t);
        setThreadLocalTransaction(null);

        assertEquals(0, v1.get());
        assertEquals(0, v2.get());
    }

    @Test
    public void rollbackScenario() {
        AlphaTransaction t = startUpdateTransaction();
        IntRef atomicObject = new IntRef(0);
        IntRefTranlocal tranlocal = (IntRefTranlocal) t.load(atomicObject);

        //start the or, and do a check that the tranlocal has not changed.
        t.startOr();
        assertSame(tranlocal, t.load(atomicObject));

        //now do an increase that is going to be rolled back.
        atomicObject.inc(tranlocal);

        //now do a rollback.
        t.endOrAndStartElse();
        //check that the inc has been rolled back.
        assertEquals(0, atomicObject.get());
    }

    @Test
    public void rollbackScenarioWithMultipleLevels() {
        IntRef v = new IntRef(0);

        Transaction t = startUpdateTransaction();
        v.inc();
        t.startOr();
        v.inc();
        t.startOr();
        v.inc();
        assertEquals(3, v.get());
        t.endOrAndStartElse();
        assertEquals(2, v.get());
        t.endOrAndStartElse();
        assertEquals(1, v.get());
        t.commit();

        setThreadLocalTransaction(null);

        assertEquals(1, v.get());
    }

    @Test
    public void multipleLevelsOfEndOr() {
        IntRef v = new IntRef(0);

        long startVersion = stm.getTime();
        Transaction t = startUpdateTransaction();
        v.inc();
        t.startOr();
        assertEquals(1, v.get());
        v.inc();

        t.startOr();
        assertEquals(2, v.get());

        t.endOr();
        assertEquals(2, v.get());

        t.endOr();
        assertEquals(2, v.get());

        t.commit();

        assertEquals(startVersion + 1, stm.getTime());
        setThreadLocalTransaction(null);
        assertEquals(2, v.get());
    }

    @Test
    public void scenarioWithNothingToCommitAfterARollback() {
        IntRef v = new IntRef(0);

        long startVersion = stm.getTime();
        Transaction t = startUpdateTransaction();
        t.startOr();
        v.inc();
        t.endOrAndStartElse();
        t.commit();

        setThreadLocalTransaction(null);

        assertEquals(startVersion, stm.getTime());
        assertIsCommitted(t);
        assertEquals(0, v.get());
    }

    @Test
    public void endOrFailsIfCalledTooEarly() {
        Transaction t = stm.startUpdateTransaction(null);

        try {
            t.endOr();
            fail();
        } catch (IllegalStateException ex) {
        }

        assertIsActive(t);
    }

    @Test
    public void endOrAndStartElseIfCalledTooEarly() {
        Transaction t = stm.startUpdateTransaction(null);

        try {
            t.endOrAndStartElse();
            fail();
        } catch (IllegalStateException ex) {
        }

        assertIsActive(t);
    }

    @Test
    public void emptyStartOrEndOrDoesNotFail() {
        long startVersion = stm.getTime();

        Transaction t = startUpdateTransaction();
        t.startOr();
        t.endOr();
        t.commit();

        assertIsCommitted(t);
        assertEquals(startVersion, stm.getTime());
    }

    @Test
    public void emptyStartOrEndOrAndStartElseDoesNotFail() {
        long startVersion = stm.getTime();

        Transaction t = startUpdateTransaction();
        t.startOr();
        t.endOrAndStartElse();
        t.commit();

        assertIsCommitted(t);
        assertEquals(startVersion, stm.getTime());
    }

    @Test
    public void startOrFailsIfTransactionAlreadyIsAborted() {
        Transaction t = stm.startUpdateTransaction(null);
        t.abort();

        try {
            t.startOr();
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsAborted(t);
    }

    @Test
    public void endOrFailsIfTransactionAlreadyIsAborted() {
        Transaction t = stm.startUpdateTransaction(null);
        t.abort();

        try {
            t.endOr();
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsAborted(t);
    }

    @Test
    public void endOrAndStartElseFailsIfTransactionAlreadyIsAborted() {
        Transaction t = stm.startUpdateTransaction(null);
        t.abort();

        try {
            t.endOrAndStartElse();
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsAborted(t);
    }

    @Test
    public void startOrFailsIfTransactionAlreadyIsCommitted() {
        Transaction t = stm.startUpdateTransaction(null);
        t.commit();

        try {
            t.startOr();
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsCommitted(t);
    }

    @Test
    public void endOrFailsIfTransactionAlreadyIsCommitted() {
        Transaction t = stm.startUpdateTransaction(null);
        t.commit();

        try {
            t.endOr();
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsCommitted(t);
    }

    @Test
    public void endOrAndStartElseFailsIfTransactionIsAborted() {
        Transaction t = stm.startUpdateTransaction(null);
        t.commit();

        try {
            t.endOrAndStartElse();
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsCommitted(t);
    }
}
