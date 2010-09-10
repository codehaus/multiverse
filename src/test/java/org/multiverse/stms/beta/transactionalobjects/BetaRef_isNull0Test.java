package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.beta.BetaStmUtils.createRef;

public class BetaRef_isNull0Test {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenNoTransactionAvailableAndNoValue() {
        BetaRef ref = createRef(stm);
        assertTrue(ref.isNull());
    }

    @Test
    public void whenNoTransactionAvailableAndValue() {
        BetaRef ref = createRef(stm, "foo");
        assertFalse(ref.isNull());
    }

    @Test
    public void whenActiveTransactionAvailable() {
        BetaRef ref = createRef(stm, "foo");

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        assertFalse(ref.isNull());
        ref.set(tx, null);
        assertTrue(ref.isNull());

        assertIsActive(tx);
    }

    @Test
    public void whenPreparedTransactionAvailable_thenPreparedTransactionException() {
        BetaRef ref = createRef(stm);
        RefTranlocal committed = ref.___unsafeLoad();

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
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenCommittedTransactionAvailable_thenCallExecutedAtomically() {
        BetaRef ref = createRef(stm);
        RefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.commit();
        setThreadLocalTransaction(tx);

        boolean result = ref.isNull();

        assertTrue(result);
        assertIsCommitted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenAbortedTransactionAvailable() {
        BetaRef ref = createRef(stm);
        RefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.abort();
        setThreadLocalTransaction(tx);

        boolean result = ref.isNull();

        assertTrue(result);
        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertSame(committed, ref.___unsafeLoad());
    }


}
