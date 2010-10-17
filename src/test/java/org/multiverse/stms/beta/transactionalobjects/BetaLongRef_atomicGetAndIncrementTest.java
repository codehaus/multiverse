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
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class BetaLongRef_atomicGetAndIncrementTest {
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

        long result = ref.atomicGetAndIncrement(1);
        assertEquals(2, result);
        assertEquals(3, ref.atomicGet());
        assertHasNoCommitLock(ref);
        assertSurplus(0, ref);
        assertNull(getThreadLocalTransaction());
    }

    @Test
    public void whenNoChange() {
        BetaLongRef ref = newLongRef(stm, 2);
        long version = ref.getVersion();

        long result = ref.atomicGetAndIncrement(0);

        assertEquals(2, result);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(0, ref);
        assertNull(getThreadLocalTransaction());
        assertVersionAndValue(ref, version, 2);
    }

    @Test
    public void whenActiveTransactionAvailable_thenIgnored() {
        int initialValue = 2;
        BetaLongRef ref = newLongRef(stm, initialValue);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.set(10);

        long result = ref.atomicGetAndIncrement(1);

        assertEquals(2, result);
        assertEquals(3, ref.atomicGet());
        assertHasNoCommitLock(ref);
        assertSurplus(1, ref);
        assertSame(tx, getThreadLocalTransaction());
        assertIsActive(tx);
    }

    @Test
    public void whenPrivatizedByOther_thenLockedException() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.privatize(otherTx);

        try {
            ref.atomicCompareAndSet(0, 1);
            fail();
        } catch (LockedException expected) {
        }

        assertSurplus(1, ref);
        assertRefHasCommitLock(ref, otherTx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenEnsuredByOther_thenLockedException() {
        int initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.ensure(otherTx);

        try {
            ref.atomicCompareAndSet(initialValue, 20);
            fail();
        } catch (LockedException expected) {
        }

        assertSurplus(1, ref);
        assertRefHasUpdateLock(ref, otherTx);
        assertVersionAndValue(ref, initialVersion, 10);
    }

    @Test
    public void whenListenersAvailable() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        int amount = 1;
        LongRefAwaitThread thread = new LongRefAwaitThread(ref, initialValue + amount);
        thread.start();

        sleepMs(500);

        long result = ref.atomicGetAndIncrement(amount);

        assertEquals(result, initialValue);
        joinAll(thread);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion + 1, initialValue + amount);
    }


}
