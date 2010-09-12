package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.newRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertSurplus;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertUnlocked;

public class BetaRef_atomicIsNullTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenNull() {
        BetaRef<String> ref = newRef(stm,null);
        RefTranlocal committed = ref.___unsafeLoad();

        boolean result = ref.atomicIsNull();

        assertTrue(result);
        assertSurplus(0, ref);
        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
    }

    public void whenActiveTransactionAvailable_thenIgnored(){
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
    @Ignore
    public void whenLocked() {
    }
}
