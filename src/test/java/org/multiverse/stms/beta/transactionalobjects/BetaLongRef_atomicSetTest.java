package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.LockedException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConfiguration;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.beta.BetaStmTestUtils.assertVersionAndValue;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class BetaLongRef_atomicSetTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        BetaStmConfiguration config = new BetaStmConfiguration();
        config.maxRetries = 10;
        stm = new BetaStm(config);
        clearThreadLocalTransaction();
    }

    @Test
    public void whenSuccess() {
        BetaLongRef ref = newLongRef(stm, 2);

        long result = ref.atomicSet(10);

        assertEquals(10, result);
        assertNull(getThreadLocalTransaction());
        assertEquals(10, ref.atomicGet());
        assertSurplus(0, ref);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertUpdateBiased(ref);
    }

    @Test
    public void whenActiveTransactionAvailable_thenIgnored() {
        BetaLongRef ref = newLongRef(stm, 2);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        long result = ref.atomicSet(10);

        assertIsActive(tx);
        assert (tx.get(ref) == null);
        assertEquals(10, result);
        assertSame(tx, getThreadLocalTransaction());
        assertEquals(10, ref.atomicGet());
        assertSurplus(0, ref);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertUpdateBiased(ref);
    }

    @Test
    public void whenPrivatizedByOther_thenLockedException() {
        BetaLongRef ref = newLongRef(stm,10);
        long version = ref.getVersion();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.privatize(otherTx);

        try {
            ref.atomicSet(1);
            fail();
        } catch (LockedException expected) {
        }

        assertSurplus(1, ref);
        assertHasCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void whenEnsuredByOtherAndChange_thenLockedException() {
        BetaLongRef ref = newLongRef(stm);
        long version = ref.getVersion();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.ensure(otherTx);

        try {
            ref.atomicSet(1);
            fail();
        } catch (LockedException expected) {
        }

        assertSurplus(1, ref);
        assertHasNoCommitLock(ref);
        assertHasUpdateLock(ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertVersionAndValue(ref, version, 0);
    }

    @Test
    public void whenNoChange_thenNoCommit() {
        BetaLongRef ref = newLongRef(stm, 2);
        long version = ref.getVersion();

        long result = ref.atomicSet(2);

        assertEquals(2, result);
        assertVersionAndValue(ref, version, 2);
        assertNull(getThreadLocalTransaction());
        assertEquals(2, ref.atomicGet());
        assertSurplus(0, ref);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertUpdateBiased(ref);
    }

    @Test
    @Ignore
    public void whenListenersAvailable() {

    }
}
