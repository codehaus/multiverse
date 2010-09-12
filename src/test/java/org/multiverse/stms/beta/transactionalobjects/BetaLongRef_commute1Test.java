package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.functions.IncLongFunction;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmUtils;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class BetaLongRef_commute1Test {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenActiveTransactionAvailable() {
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        LongFunction function = new IncLongFunction(1);
        ref.commute(function);

        LongRefTranlocal commuting = (LongRefTranlocal) tx.get(ref);
        assertNotNull(commuting);
        assertTrue(commuting.isCommuting);
        assertFalse(commuting.isCommitted);
        assertSurplus(0, ref);
        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());
        assertEquals(0, commuting.value);
        assertIsActive(tx);
        assertSame(tx, getThreadLocalTransaction());
        tx.commit();

        assertEquals(1, ref.get());
        assertIsCommitted(tx);
        assertSurplus(0, ref);
        assertUnlocked(ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenActiveTransactionAvailableAndNoChange() {
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();
        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        LongFunction function = new IdentityLongFunction();
        ref.commute(function);

        LongRefTranlocal commuting = (LongRefTranlocal) tx.get(ref);
        assertNotNull(commuting);
        assertTrue(commuting.isCommuting);
        assertFalse(commuting.isCommitted);
        assertSurplus(0, ref);
        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());
        assertEquals(0, commuting.value);
        assertIsActive(tx);
        assertSame(tx, getThreadLocalTransaction());
        tx.commit();

        assertEquals(0, ref.get());
        assertSame(committed, ref.___unsafeLoad());
        assertIsCommitted(tx);
        assertSurplus(0, ref);
        assertUnlocked(ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenActiveTransactionAvailableAndNullFunction_thenNullPointerException() {
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();
        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        try {
            ref.commute(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertSame(committed, ref.___unsafeLoad());
        assertIsAborted(tx);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertUnlocked(ref);
        assertEquals(0, ref.get());
    }

    @Test
    @Ignore
    public void whenNoTransactionAvailable() {

    }

    @Test
    @Ignore
    public void whenCommittedTransactionAvailable_thenExecuteAtomically() {

    }

    @Test
    @Ignore
    public void whenAbortedTransactionAvailable_thenExecuteAtomically() {

    }

    @Test
    @Ignore
    public void whenPreparedTransactionAvailable_thenPreparedTransactionException() {

    }

}
