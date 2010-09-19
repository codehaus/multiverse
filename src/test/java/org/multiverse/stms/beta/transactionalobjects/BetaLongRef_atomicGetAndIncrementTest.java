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
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
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
        LongRefTranlocal committed = ref.___unsafeLoad();

        long result = ref.atomicGetAndIncrement(0);

        assertEquals(2, result);
        assertEquals(2, ref.atomicGet());
        assertHasNoCommitLock(ref);
        assertSurplus(0, ref);
        assertNull(getThreadLocalTransaction());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenActiveTransactionAvailable_thenIgnored() {
        BetaLongRef ref = newLongRef(stm, 2);

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
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.privatize(otherTx);

        try {
            ref.atomicCompareAndSet(0, 1);
            fail();
        } catch (LockedException expected) {
        }

        assertSurplus(1, ref);
        assertHasCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenEnsuredByOther_thenLockedException() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.ensure(otherTx);

        try {
            ref.atomicCompareAndSet(0, 1);
            fail();
        } catch (LockedException expected) {
        }

        assertSurplus(1, ref);
        assertHasNoCommitLock(ref);
        assertHasUpdateLock(ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    @Ignore
    public void whenListenersAvailable() {

    }


}
