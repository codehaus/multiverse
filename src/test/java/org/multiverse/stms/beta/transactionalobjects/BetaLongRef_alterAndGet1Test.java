package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.functions.IncLongFunction;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

public class BetaLongRef_alterAndGet1Test {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenActiveTransactionAvailableAndNullFunction_thenNullPointerException() {
        BetaLongRef ref = createLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        try {
            ref.alterAndGet(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTransaction());
    }

    @Test
    public void whenActiveTransactionAvailable() {
        BetaLongRef ref = createLongRef(stm);

        LongFunction function = IncLongFunction.INSTANCE;
        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        ref.alterAndGet(function);
        assertEquals(1, ref.get());
        assertEquals(0, ref.atomicGet());
        tx.commit();

        assertEquals(1, ref.get());
    }

    @Test
    public void whenPreparedTransactionAvailable_thenPreparedTransactionException() {
        BetaLongRef ref = createLongRef(stm);

        LongFunction function = mock(LongFunction.class);
        BetaTransaction tx = stm.startDefaultTransaction();
        tx.prepare();
        setThreadLocalTransaction(tx);

        try {
            ref.alterAndGet(function);
            fail();
        } catch (PreparedTransactionException expected) {

        }

        assertIsAborted(tx);
        verifyZeroInteractions(function);
        assertEquals(0, ref.get());
    }

    @Test
    @Ignore
    public void whenNoTransactionAvailable() {

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
