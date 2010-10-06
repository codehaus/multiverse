package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.LockedException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.assertVersionAndValue;
import static org.multiverse.stms.beta.BetaStmUtils.newRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class BetaRef_atomicIsNullTest implements BetaStmConstants {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test(expected = LockedException.class)
    public void whenPrivatized_thenLockedException() {
        BetaRef<String> ref = newRef(stm, null);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.privatize(tx);

        ref.atomicIsNull();
    }

    @Test
    public void whenEnsured_thenSuccess() {
        BetaRef<String> ref = newRef(stm, null);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.ensure(tx);

        boolean result = ref.atomicIsNull();
        assertTrue(result);
    }

    @Test
    public void whenNull() {
        BetaRef<String> ref = newRef(stm, null);
        long initialVersion = ref.getVersion();


        boolean result = ref.atomicIsNull();

        assertTrue(result);
        assertSurplus(0, ref);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertVersionAndValue(ref, initialVersion, null);
    }

    @Test
    public void whenActiveTransactionAvailable_thenIgnored() {
        String initialValue = "foo";
        BetaRef<String> ref = newRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.set(tx, null);

        boolean result = ref.atomicIsNull();

        assertFalse(result);
        assertSurplus(1, ref);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenNotNull() {
        String initialValue = "foo";
        BetaRef<String> ref = newRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        boolean result = ref.atomicIsNull();

        assertFalse(result);
        assertSurplus(0, ref);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenAlreadyPrivatized() {
        BetaRef<String> ref = newRef(stm, "foo");

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.privatize(tx);

        try {
            ref.atomicIsNull();
            fail();
        } catch (LockedException expected) {
        }

        assertSurplus(1, ref);
        assertHasCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertSame(tx, ref.___getLockOwner());
        assertIsActive(tx);
    }

    @Test
    public void whenAlreadyEnsured() {
        BetaRef<String> ref = newRef(stm, "foo");

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.ensure(tx);

        boolean result = ref.atomicIsNull();

        assertFalse(result);
        assertSurplus(1, ref);
        assertHasNoCommitLock(ref);
        assertHasUpdateLock(ref);
        assertSame(tx, ref.___getLockOwner());
        assertIsActive(tx);
    }
}
