package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.LockedException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.newRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class BetaRef_atomicIsNullTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenNull() {
        BetaRef<String> ref = newRef(stm, null);
        RefTranlocal committed = ref.___unsafeLoad();

        boolean result = ref.atomicIsNull();

        assertTrue(result);
        assertSurplus(0, ref);
        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
    }

    public void whenActiveTransactionAvailable_thenIgnored() {
        BetaRef<String> ref = newRef(stm, "foo");
        RefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.set(tx, null);

        boolean result = ref.atomicIsNull();

        assertFalse(result);
        assertSurplus(1, ref);
        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenNotNull() {
        BetaRef<String> ref = newRef(stm, "foo");
        RefTranlocal committed = ref.___unsafeLoad();

        boolean result = ref.atomicIsNull();

        assertFalse(result);
        assertSurplus(0, ref);
        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenLocked() {
        BetaRef<String> ref = newRef(stm, "foo");

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.openForRead(ref, true);

        try {
            ref.atomicIsNull();
            fail();
        } catch (LockedException expected) {
        }

        assertSurplus(1, ref);
        assertLocked(ref);
        assertSame(tx, ref.___getLockOwner());
        assertIsActive(tx);
    }
}
