package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.WriteConflict;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class BetaLongRef_atomicSetTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenSuccess() {
        BetaLongRef ref = createLongRef(stm, 2);

        long result = ref.atomicSet(10);

        assertEquals(10, result);
        assertNull(getThreadLocalTransaction());
        assertEquals(10, ref.get());
        assertSurplus(0, ref);
        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());
        assertUpdateBiased(ref);
    }

    @Test
    public void whenActiveTransactionAvailable_thenIgnored() {
        BetaLongRef ref = createLongRef(stm, 2);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        long result = ref.atomicSet(10);

        assertIsActive(tx);
        assert (tx.get(ref) == null);
        assertEquals(10, result);
        assertSame(tx, getThreadLocalTransaction());
        assertEquals(10, ref.atomicGet());
        assertSurplus(0, ref);
        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());
        assertUpdateBiased(ref);
    }

    @Test
    @Ignore
    public void whenListenersAvailable() {

    }

    @Test
    public void whenLocked_thenWriteConflict() {
        BetaLongRef ref = createLongRef(stm, 2);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.openForRead(ref, true);

        try {
            ref.atomicSet(10);
            fail();
        } catch (WriteConflict e) {

        }

        assertSame(committed, ref.___unsafeLoad());
        assertNull(getThreadLocalTransaction());
        assertSurplus(1, ref);
        assertLocked(ref);
        assertSame(tx,ref.___getLockOwner());
        assertUpdateBiased(ref);
    }

    @Test
    public void whenNoChange_thenNoCommit() {
        BetaLongRef ref = createLongRef(stm, 2);
        LongRefTranlocal committed = ref.___unsafeLoad();

        long result = ref.atomicSet(2);

        assertEquals(2, result);
        assertSame(committed, ref.___unsafeLoad());
        assertNull(getThreadLocalTransaction());
        assertEquals(2, ref.atomicGet());
        assertSurplus(0, ref);
        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());
        assertUpdateBiased(ref);
    }
}
