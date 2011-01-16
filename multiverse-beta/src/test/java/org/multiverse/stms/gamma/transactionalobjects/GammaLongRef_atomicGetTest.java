package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.LockMode;
import org.multiverse.api.exceptions.LockedException;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public class GammaLongRef_atomicGetTest {

     private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
        clearThreadLocalTransaction();
    }

    @Test(expected = LockedException.class)
    @Ignore
    public void whenUnconstructed() {
        GammaTransaction tx = stm.startDefaultTransaction();
        //GammaLongRef ref = new GammaLongRef(tx);
        //ref.atomicGet();
    }

    @Test
    public void whenActiveTransactionAvailable_thenIgnored() {
        GammaLongRef ref = new GammaLongRef(stm, 100);

        GammaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.set(10);

        assertEquals(100, ref.atomicGet());

        assertSame(tx, getThreadLocalTransaction());
    }

    @Test
    public void whenUpdatedBiasedOnUnlocked() {
        GammaLongRef ref = new GammaLongRef(stm, 100);

        long result = ref.atomicGet();
        assertEquals(100, result);
        //assertUpdateBiased(ref);
    }

    @Test
    public void whenUpdateBiasedAndPrivatizedByOther_thenLockedException() {
        GammaLongRef ref = new GammaLongRef(stm, 100);
        long version = ref.getVersion();

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Commit);

        try {
            ref.atomicGet();
            fail();
        } catch (LockedException ex) {
        }

        assertSurplus(ref, 1);
        assertUpdateBiased(ref);
        assertRefHasCommitLock(ref, otherTx);
        assertVersionAndValue(ref, version, 100);
    }

    @Test
    public void whenUpdateBiasedAndEnsuredByOther() {
        GammaLongRef ref = new GammaLongRef(stm, 100);
        long version = ref.getVersion();

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx,LockMode.Write);

        long result = ref.atomicGet();

        assertEquals(100, result);
        assertSurplus(ref, 1);
        assertRefHasWriteLock(ref, otherTx);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, version, 100);
    }

    @Test
    public void whenReadBiasedAndUnlocked() {
        GammaLongRef ref = makeReadBiased(new GammaLongRef(stm, 100));

        long result = ref.atomicGet();
        assertEquals(100, result);
        assertReadBiased(ref);
    }

    @Test
    @Ignore
    public void whenReadBiasedAndPrivatizedByOther_thenLockedException() {
        GammaLongRef ref = makeReadBiased(new GammaLongRef(stm, 100));
        long version = ref.getVersion();

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx,LockMode.Commit);

        try {
            ref.atomicGet();
            fail();
        } catch (LockedException ex) {
        }

        assertSurplus(ref, 1);
        assertReadBiased(ref);
        assertRefHasCommitLock(ref, otherTx);
        assertVersionAndValue(ref, version, 100);
    }

    @Test
    @Ignore
    public void whenReadBiasedAndEnsuredByOther_thenLockedException() {
        GammaLongRef ref = makeReadBiased(new GammaLongRef(stm, 100));
        long version = ref.getVersion();

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx,LockMode.Commit);

        long result = ref.atomicGet();

        assertEquals(100, result);
        assertSurplus(ref, 1);
        assertRefHasWriteLock(ref, otherTx);
        assertReadBiased(ref);
        assertVersionAndValue(ref, version, 100);
    }
}
