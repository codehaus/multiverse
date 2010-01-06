package org.multiverse.stms.alpha;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.LoadTooOldVersionException;
import org.multiverse.api.exceptions.LoadUncommittedException;
import org.multiverse.stms.alpha.manualinstrumentation.IntRef;
import org.multiverse.stms.alpha.manualinstrumentation.IntRefTranlocal;

/**
 * @author Peter Veentjer
 */
public class ReadonlyAlphaTransaction_loadTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = AlphaStm.createDebug();
        setGlobalStmInstance(stm);
        setThreadLocalTransaction(null);
    }

    @After
    public void after() {
        setThreadLocalTransaction(null);
    }

    public AlphaTransaction startUpdateTransaction() {
        AlphaTransaction t = stm.startUpdateTransaction(null);
        setThreadLocalTransaction(t);
        return t;
    }

    public AlphaTransaction startReadonlyTransaction() {
        AlphaTransaction t = stm.startReadOnlyTransaction(null);
        setThreadLocalTransaction(t);
        return t;
    }

    //====================== loadReadonly ====================================

    @Test
    public void loadNullReturnsNull() {
        AlphaTransaction t = startReadonlyTransaction();
        AlphaTranlocal result = t.load(null);
        assertNull(result);
    }

    @Test
    public void loadNonCommitted() {
        IntRef value = IntRef.createUncommitted();

        AlphaTransaction t = startReadonlyTransaction();

        try {
            t.load(value);
            fail();
        } catch (LoadUncommittedException ex) {
        }

        assertIsActive(t);
    }

    @Test
    public void loadPreviouslyCommitted() {
        IntRef value = new IntRef(10);

        IntRefTranlocal expected = (IntRefTranlocal) value.___load(stm.getTime());

        AlphaTransaction t2 = startReadonlyTransaction();
        IntRefTranlocal found = (IntRefTranlocal) t2.load(value);
        assertTrue(found.___writeVersion > 0);
        assertSame(expected, found);
    }

    @Test
    public void loadDoesNotObserveChangesMadeByOtherTransactions() {
        IntRef ref = new IntRef(0);

        AlphaTransaction readonlyTransaction = stm.startReadOnlyTransaction(null);
        AlphaTransaction updateTransaction = stm.startUpdateTransaction(null);
        IntRefTranlocal tranlocal = (IntRefTranlocal) updateTransaction.load(ref);
        ref.inc(tranlocal);

        IntRefTranlocal tranlocalIntValue = (IntRefTranlocal) readonlyTransaction.load(ref);
        assertEquals(0, ref.get(tranlocalIntValue));
    }

    /**
     * Since readonly transactions does not track reads (see the {@linkplain ReadonlyAlphaTransaction JavaDoc}), it will
     * immediately see a <em>committed</em> change made by another transaction.
     * <p/>
     * If read tracking is implemented this behaviour is expected to change, i.e. loads after commits by other
     * transactions should still succeed and return the value that was current when the readonly transaction started.
     */
    @Test
    public void loadObservesCommittedChangesMadeByOtherTransactions() {
        IntRef ref = new IntRef(0);

        AlphaTransaction readonlyTransaction = stm.startReadOnlyTransaction(null);
        AlphaTransaction updateTransaction = stm.startUpdateTransaction(null);
        IntRefTranlocal tranlocal = (IntRefTranlocal) updateTransaction.load(ref);
        ref.inc(tranlocal);

        // will succeed because the updating transaction hasn't committed yet
        IntRefTranlocal tranlocalIntValue = (IntRefTranlocal) readonlyTransaction.load(ref);
        assertEquals(0, ref.get(tranlocalIntValue));

        updateTransaction.commit();

        // will fail because the version requested is too old (no read tracking)
        try {
            readonlyTransaction.load(ref);
            fail();
        } catch (LoadTooOldVersionException ex) {
        }
    }

    @Test
    public void loadOnCommittedTransactionFails() {
        IntRef value = new IntRef(10);

        AlphaTransaction t = startReadonlyTransaction();
        t.commit();

        try {
            t.load(value);
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsCommitted(t);
    }


    @Test
    public void loadOnAbortedTransactionFails() {
        IntRef value = new IntRef(10);

        AlphaTransaction t = startReadonlyTransaction();
        t.abort();

        try {
            t.load(value);
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsAborted(t);
    }
}
