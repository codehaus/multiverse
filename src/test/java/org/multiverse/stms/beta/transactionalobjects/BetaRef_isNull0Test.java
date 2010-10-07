package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.PreparedTransactionException;
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
    public void whenNoTransactionAvailableAndNoValue() {
        BetaRef ref = newRef(stm);
        assertTrue(ref.isNull());
    }

    @Test
    public void whenNoTransactionAvailableAndValue() {
        BetaRef ref = newRef(stm, "foo");
        assertFalse(ref.isNull());
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
    public void whenCommittedTransactionAvailable_thenCallExecutedAtomically() {
        BetaRef ref = newRef(stm);
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.commit();
        setThreadLocalTransaction(tx);

        boolean result = ref.isNull();

        assertTrue(result);
        assertIsCommitted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, version, null);
    }

    @Test
    public void whenAbortedTransactionAvailable() {
        BetaRef ref = newRef(stm);
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.abort();
        setThreadLocalTransaction(tx);

        boolean result = ref.isNull();

        assertTrue(result);
        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, version, null);
    }
}
