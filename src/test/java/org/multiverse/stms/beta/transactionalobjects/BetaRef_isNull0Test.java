package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.TransactionRequiredException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.beta.BetaStmTestUtils.assertVersionAndValue;
import static org.multiverse.stms.beta.BetaStmTestUtils.newRef;

public class BetaRef_isNull0Test {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenNoTransactionAvailableAndNullValue_thenNoTransactionFoundException() {
        BetaRef ref = newRef(stm);
        long initialVersion = ref.getVersion();

        try {
            ref.isNull();
            fail();
        } catch (TransactionRequiredException expected) {
        }

        assertVersionAndValue(ref, initialVersion, null);
    }

    @Test
    public void whenNoTransactionAvailableAndValue_thenNoTransactionFoundException() {
        String initialValue = "foo";
        BetaRef ref = newRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        try {
            ref.isNull();
            fail();
        } catch (TransactionRequiredException expected) {

        }

        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenActiveTransactionAvailable() {
        BetaRef<String> ref = newRef(stm, "foo");

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        assertFalse(ref.isNull());
        ref.set(tx, null);
        assertTrue(ref.isNull());

        assertIsActive(tx);
    }

    @Test
    public void whenPreparedTransactionAvailable_thenPreparedTransactionException() {
        String value = "foo";
        BetaRef ref = newRef(stm, value);
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        tx.prepare();

        try {
            ref.isNull();
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertEquals(version, ref.getVersion());
        assertVersionAndValue(ref, version, value);
    }

    @Test
    public void whenCommittedTransactionAvailable_thenDeadTransactionException() {
        String initialValue = "foo";
        BetaRef ref = newRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.commit();
        setThreadLocalTransaction(tx);

        try {
            ref.isNull();
            fail();
        } catch (DeadTransactionException expected) {

        }

        assertIsCommitted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenAbortedTransactionAvailable_thenDeadTransactionException() {
        String initialValue = "foo";
        BetaRef<String> ref = newRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.abort();
        setThreadLocalTransaction(tx);

        try {
            ref.isNull();
            fail();
        } catch (DeadTransactionException expected) {

        }

        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, initialVersion, initialValue);
    }
}
