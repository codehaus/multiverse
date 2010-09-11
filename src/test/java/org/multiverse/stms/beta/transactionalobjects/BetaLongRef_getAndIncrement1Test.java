package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.references.LongRef;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsCommitted;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertSurplus;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertUnlocked;

public class BetaLongRef_getAndIncrement1Test {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenPreparedTransactionAvailable_thenPreparedTransactionException() {
        BetaLongRef ref = createLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.prepare();
        setThreadLocalTransaction(tx);

        try {
            ref.getAndIncrement(30);
            fail();
        } catch (PreparedTransactionException expected) {

        }

        assertIsAborted(tx);
        assertEquals(10, ref.atomicGet());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenActiveTransactionAvailable() {
        LongRef ref = createLongRef(stm, 10);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        long value = ref.getAndIncrement(20);
        tx.commit();

        assertEquals(10, value);
        assertIsCommitted(tx);
        assertEquals(30, ref.atomicGet());
        assertSame(tx, getThreadLocalTransaction());
    }

    @Test
    public void whenNoChange() {
        BetaLongRef ref = createLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        long value = ref.getAndIncrement(0);
        tx.commit();

        assertEquals(10, value);
        assertIsCommitted(tx);
        assertEquals(10, ref.atomicGet());
        assertSame(tx, getThreadLocalTransaction());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    @Ignore
    public void whenListenersAvailable() {

    }

    @Test
    public void whenNoTransactionAvailable_thenExecutedAtomically() {
        BetaLongRef ref = createLongRef(stm, 10);

        long result = ref.getAndIncrement(10);

        assertSurplus(0, ref);
        assertUnlocked(ref);
        assertEquals(result, 10);
        assertEquals(20, ref.atomicGet());
        assertNull(getThreadLocalTransaction());
    }

    @Test
    public void whenCommittedTransactionAvailable() {
        BetaLongRef ref = createLongRef(stm, 10);
        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        tx.commit();

        long result = ref.getAndIncrement(10);

        assertSurplus(0, ref);
        assertUnlocked(ref);
        assertEquals(result, 10);
        assertEquals(20, ref.atomicGet());
        assertSame(tx, getThreadLocalTransaction());
        assertIsCommitted(tx);
    }

    @Test
    public void whenAbortedTransactionAvailable() {
        BetaLongRef ref = createLongRef(stm, 10);
        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        tx.abort();

        long result = ref.getAndIncrement(10);

        assertSurplus(0, ref);
        assertUnlocked(ref);
        assertEquals(result, 10);
        assertEquals(20, ref.atomicGet());
        assertSame(tx, getThreadLocalTransaction());
        assertIsAborted(tx);
    }

}
