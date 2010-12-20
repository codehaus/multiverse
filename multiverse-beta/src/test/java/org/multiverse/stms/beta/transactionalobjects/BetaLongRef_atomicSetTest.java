package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.LockedException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConfiguration;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.beta.BetaStmTestUtils.*;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertSurplus;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertUpdateBiased;

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
        long initialValue = 2;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        int newValue = 10;
        long result = ref.atomicSet(newValue);

        assertEquals(newValue, result);
        assertNull(getThreadLocalTransaction());
        assertSurplus(0, ref);
        assertRefHasNoLocks(ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, initialVersion+1, newValue);
    }

    @Test
    public void whenActiveTransactionAvailable_thenIgnored() {
        long initialValue = 2;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        long newValue = 10;
        long result = ref.atomicSet(newValue);

        assertIsActive(tx);
        assert (tx.get(ref) == null);
        assertEquals(newValue, result);
        assertSame(tx, getThreadLocalTransaction());
        assertSurplus(0, ref);
        assertRefHasNoLocks(ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, initialVersion+1,newValue);
    }

    @Test
    public void whenPrivatizedByOther_thenLockedException() {
        int initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquireCommitLock(otherTx);

        try {
            ref.atomicSet(1);
            fail();
        } catch (LockedException expected) {
        }

        assertSurplus(1, ref);
        assertRefHasCommitLock(ref, otherTx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenEnsuredByOtherAndChange_thenLockedException() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquireWriteLock(otherTx);

        try {
            ref.atomicSet(1);
            fail();
        } catch (LockedException expected) {
        }

        assertSurplus(1, ref);
        assertRefHasUpdateLock(ref, otherTx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenNoChange_thenNoCommit() {
        long initialValue = 2;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        long result = ref.atomicSet(initialValue);

        assertEquals(initialValue, result);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertNull(getThreadLocalTransaction());
        assertSurplus(0, ref);
        assertRefHasNoLocks(ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenListenersAvailable() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        long newValue = initialValue + 1;
        LongRefAwaitThread thread = new LongRefAwaitThread(ref, newValue);
        thread.start();

        sleepMs(500);

        long result = ref.atomicSet(newValue);

        assertEquals(newValue, result);
        joinAll(thread);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion + 1, newValue);
    }
}
