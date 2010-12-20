package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.LockedException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConfiguration;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.sleepMs;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.beta.BetaStmTestUtils.*;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertSurplus;

public class BetaLongRef_atomicIncrementAndGetTest {
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
        int initialValue = 2;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        int amount = 10;
        long result = ref.atomicIncrementAndGet(amount);

        assertEquals(initialValue + amount, result);
        assertRefHasNoLocks(ref);
        assertSurplus(0, ref);
        assertVersionAndValue(ref, initialVersion + 1, initialValue + amount);
        assertNull(getThreadLocalTransaction());
    }

    @Test
    public void whenActiveTransactionAvailable_thenIgnored() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.set(tx, 1000);

        long amount = 1;
        long result = ref.atomicIncrementAndGet(amount);

        assertEquals(initialValue+amount, result);
        assertRefHasNoLocks(ref);
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, initialVersion + 1, initialValue + amount);
    }

    @Test
    public void whenNoChange() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        long result = ref.atomicIncrementAndGet(0);

        assertEquals(initialValue, result);
        assertRefHasNoLocks(ref);
        assertSurplus(0, ref);
        assertNull(getThreadLocalTransaction());
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenPrivatizedByOther_thenLockedException() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquireCommitLock(otherTx);

        try {
            ref.atomicIncrementAndGet(1);
            fail();
        } catch (LockedException expected) {
        }

        assertSurplus(1, ref);
        assertRefHasCommitLock(ref, otherTx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenEnsuredByOtherAndChange_thenLockedException() {
        int initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquireWriteLock(otherTx);

        try {
            ref.atomicIncrementAndGet(1);
            fail();
        } catch (LockedException expected) {
        }

        assertSurplus(1, ref);
        assertRefHasUpdateLock(ref, otherTx);
        assertVersionAndValue(ref, initialVersion, initialValue);
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

        ref.atomicIncrementAndGet(1);

        joinAll(thread);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion + 1, newValue);
    }
}
