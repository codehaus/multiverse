package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.gamma.GammaTestUtils.assertRefHasNoLocks;
import static org.multiverse.stms.gamma.GammaTestUtils.assertVersionAndValue;

public class GammaRef_setTest {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void test() {
        String initialValue = "foo";
        GammaRef<String> ref = new GammaRef<String>(stm, initialValue);
        long initialVersion = ref.getVersion();
        long initialOrec = ref.orec;

        GammaTransaction tx = stm.startDefaultTransaction();
        String newValue = "bar";
        String result = ref.set(tx, newValue);

        assertSame(newValue, result);
        assertIsActive(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenTransactionPrepared_thenPreparedTransactionException() {
        String initialValue = "foo";
        GammaRef<String> ref = new GammaRef<String>(stm, initialValue);
        long initialVersion = ref.getVersion();
        long initialOrec = ref.orec;

        GammaTransaction tx = stm.startDefaultTransaction();
        tx.prepare();
        try {
            ref.set(tx, "bar");
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertEquals(initialOrec, ref.orec);
    }

    @Test
    public void whenTransactionAborted_thenDeadTransactionException() {
        String initialValue = "foo";
        GammaRef<String> ref = new GammaRef<String>(stm, initialValue);
        long initialVersion = ref.getVersion();
        long initialOrec = ref.orec;

        GammaTransaction tx = stm.startDefaultTransaction();
        tx.abort();
        try {
            ref.set(tx, "bar");
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertEquals(initialOrec, ref.orec);
    }

    @Test
    public void whenTransactionCommitted_thenDeadTransactionException() {
        String initialValue = "foo";
        GammaRef<String> ref = new GammaRef<String>(stm, initialValue);
        long initialVersion = ref.getVersion();
        long initialOrec = ref.orec;

        GammaTransaction tx = stm.startDefaultTransaction();
        tx.commit();
        try {
            ref.set(tx, "bar");
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertEquals(initialOrec, ref.orec);
    }
}
