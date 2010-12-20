package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.LockedException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.beta.BetaStmTestUtils.*;
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

    @Test(expected = LockedException.class)
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
        long version = ref.getVersion();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquireCommitLock(otherTx);

        try {
            ref.atomicGet();
            fail();
        } catch (LockedException ex) {
        }

        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertRefHasCommitLock(ref, otherTx);
        assertVersionAndValue(ref, version, 100);
    }

    @Test
    public void whenUpdateBiasedAndEnsuredByOther() {
        BetaLongRef ref = newLongRef(stm, 100);
        long version = ref.getVersion();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquireWriteLock(otherTx);

        long result = ref.atomicGet();

        assertEquals(100, result);
        assertSurplus(1, ref);
        assertRefHasUpdateLock(ref, otherTx);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, version, 100);
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
        long version = ref.getVersion();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquireCommitLock(otherTx);

        try {
            ref.atomicGet();
            fail();
        } catch (LockedException ex) {
        }

        assertSurplus(1, ref);
        assertReadBiased(ref);
        assertRefHasCommitLock(ref, otherTx);
        assertVersionAndValue(ref, version, 100);
    }

    @Test
    public void whenReadBiasedAndEnsuredByOther_thenLockedException() {
        BetaLongRef ref = makeReadBiased(newLongRef(stm, 100));
        long version = ref.getVersion();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquireWriteLock(otherTx);

        long result = ref.atomicGet();

        assertEquals(100, result);
        assertSurplus(1, ref);
        assertRefHasUpdateLock(ref, otherTx);
        assertReadBiased(ref);
        assertVersionAndValue(ref, version, 100);
    }
}
