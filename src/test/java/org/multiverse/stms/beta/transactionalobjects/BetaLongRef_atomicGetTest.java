package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.LockedException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
import static org.multiverse.stms.beta.BetaStmUtils.newReadBiasedLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class BetaLongRef_atomicGetTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test(expected = IllegalStateException.class)
    public void whenUnconstructed() {
        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        ref.atomicGet();
    }

    @Test
    public void whenActiveTransactionAvailable_thenIgnored() {
        BetaLongRef ref = newLongRef(stm, 100);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.set(10);

        assertEquals(100, ref.atomicGet());

        assertSame(tx, getThreadLocalTransaction());
    }

    @Test
    public void whenUpdatedBiasedOnUnlocked() {
        BetaLongRef ref = newLongRef(stm, 100);

        long result = ref.atomicGet();
        assertEquals(100, result);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenUpdateBiasedAndPrivatizedByOther_thenLockedException() {
        BetaLongRef ref = newLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.privatize(otherTx);

        try {
            ref.atomicGet();
            fail();
        } catch (LockedException ex) {
        }

        assertSame(otherTx, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertHasNoUpdateLock(ref);
        assertHasCommitLock(ref);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenUpdateBiasedAndEnsuredByOther_thenLockedException() {
        BetaLongRef ref = newLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.ensure(otherTx);

        long result = ref.atomicGet();

        assertEquals(100, result);
        assertSurplus(1, ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertUpdateBiased(ref);
        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenReadBiasedAndUnlocked() {
        BetaLongRef ref = newReadBiasedLongRef(stm, 100);

        long result = ref.atomicGet();
        assertEquals(100, result);
        assertReadBiased(ref);
    }

    @Test
    public void whenReadBiasedAndPrivatizedByOther_thenLockedException() {
        BetaLongRef ref = makeReadBiased(newLongRef(stm, 100));
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.privatize(otherTx);

        try {
            ref.atomicGet();
            fail();
        } catch (LockedException ex) {
        }

        assertSame(otherTx, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertReadBiased(ref);
        assertHasNoUpdateLock(ref);
        assertHasCommitLock(ref);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenReadBiasedAndEnsuredByOther_thenLockedException() {
        BetaLongRef ref = makeReadBiased(newLongRef(stm, 100));
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.ensure(otherTx);

        long result = ref.atomicGet();

        assertEquals(100, result);
        assertSurplus(1, ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertReadBiased(ref);
        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSame(committed, ref.___unsafeLoad());
    }
}
