package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.LockMode;
import org.multiverse.api.exceptions.LockedException;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public class GammaRef_atomicIsNullTest {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
        clearThreadLocalTransaction();
    }

    @Test(expected = LockedException.class)
    public void whenPrivatized_thenLockedException() {
        GammaRef<String> ref = new GammaRef<String>(stm, null);

        GammaTransaction tx = stm.newDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Exclusive);

        ref.atomicIsNull();
    }

    @Test
    public void whenEnsured_thenSuccess() {
        GammaRef<String> ref = new GammaRef<String>(stm, null);

        GammaTransaction tx = stm.newDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Write);

        boolean result = ref.atomicIsNull();
        assertTrue(result);
    }

    @Test
    public void whenNull() {
        GammaRef<String> ref = new GammaRef<String>(stm, null);
        long initialVersion = ref.getVersion();


        boolean result = ref.atomicIsNull();

        assertTrue(result);
        assertSurplus(ref, 0);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, null);
    }

    @Test
    public void whenActiveTransactionAvailable_thenIgnored() {
        String initialValue = "foo";
        GammaRef<String> ref = new GammaRef<String>(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.newDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.set(tx, null);

        boolean result = ref.atomicIsNull();

        assertFalse(result);
        assertSurplus(ref, 0);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenNotNull() {
        String initialValue = "foo";
        GammaRef<String> ref = new GammaRef<String>(stm, initialValue);
        long initialVersion = ref.getVersion();

        boolean result = ref.atomicIsNull();

        assertFalse(result);
        assertSurplus(ref, 0);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenAlreadyPrivatized() {
        GammaRef<String> ref = new GammaRef<String>(stm, "foo");

        GammaTransaction tx = stm.newDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Exclusive);

        try {
            ref.atomicIsNull();
            fail();
        } catch (LockedException expected) {
        }

        assertSurplus(ref, 1);
        assertRefHasExclusiveLock(ref, tx);
        assertIsActive(tx);
    }

    @Test
    public void whenAlreadyEnsured() {
        GammaRef<String> ref = new GammaRef<String>(stm, "foo");

        GammaTransaction tx = stm.newDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Write);

        boolean result = ref.atomicIsNull();

        assertFalse(result);
        assertSurplus(ref, 1);
        assertRefHasWriteLock(ref, tx);
        assertIsActive(tx);
    }
}
