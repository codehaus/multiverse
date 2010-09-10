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
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class BetaLongRef_set1Test {

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
            ref.set(30);
            fail();
        } catch (PreparedTransactionException expected) {

        }

        assertIsAborted(tx);
        assertEquals(10, ref.atomicGet());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenActiveTransactionAvailable_thenPreparedTransactionException() {
        LongRef ref = createLongRef(stm, 10);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        long value = ref.set(20);
        tx.commit();

        assertEquals(20, value);
        assertIsCommitted(tx);
        assertEquals(20, ref.atomicGet());
        assertSame(tx, getThreadLocalTransaction());
    }

    @Test
    @Ignore
    public void whenLocked() {

    }

    @Test
    @Ignore
    public void whenNoChange() {
    }

    @Test
    public void whenSuccess() {
        BetaLongRef ref = createLongRef(stm, 10);

        long result = ref.set(20);

        assertEquals(10, result);
        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertEquals(20, ref.atomicGet());
    }

    @Test
    @Ignore
    public void whenCommittedTransactionAvailable() {

    }

    @Test
    @Ignore
    public void whenAbortedTransactionAvailable() {

    }

}
