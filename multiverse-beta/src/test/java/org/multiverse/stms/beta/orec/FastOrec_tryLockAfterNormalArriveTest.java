package org.multiverse.stms.beta.orec;

import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.PanicError;

import static org.junit.Assert.*;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class FastOrec_tryLockAfterNormalArriveTest {

    @Test
    public void whenUpdateBiasedAndNoSurplus_thenPanicErrorWhenCommitLockAcquired() {
        FastOrec orec = new FastOrec();

        try {
            orec.___tryLockAfterNormalArrive(1, true);
            fail();
        } catch (PanicError expected) {
        }

        assertReadonlyCount(0, orec);
        assertSurplus(0, orec);
        assertUpdateBiased(orec);
        assertHasNoUpdateLock(orec);
        assertHasNoCommitLock(orec);
    }

    @Test
    public void whenUpdateBiasedAndNoSurplus_thenPanicErrorWhenUpdateLockAcquired() {
        FastOrec orec = new FastOrec();

        try {
            orec.___tryLockAfterNormalArrive(1, false);
            fail();
        } catch (PanicError expected) {
        }

        assertReadonlyCount(0, orec);
        assertSurplus(0, orec);
        assertUpdateBiased(orec);
        assertHasNoUpdateLock(orec);
        assertHasNoCommitLock(orec);
    }

    @Test
    public void whenUpdateBiasedAndFree_thenCommitLockCanBeAcquired() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);

        boolean result = orec.___tryLockAfterNormalArrive(1, true);

        assertTrue(result);
        assertReadonlyCount(0, orec);
        assertSurplus(1, orec);
        assertUpdateBiased(orec);
        assertHasNoUpdateLock(orec);
        assertHasCommitLock(orec);
    }

    @Test
    public void whenUpdateBiasedAndFree_thenUpdateLockCanBeAcquired() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);

        boolean result = orec.___tryLockAfterNormalArrive(1, false);

        assertTrue(result);
        assertReadonlyCount(0, orec);
        assertSurplus(1, orec);
        assertUpdateBiased(orec);
        assertHasUpdateLock(orec);
        assertHasNoCommitLock(orec);
    }

    @Test
    public void whenUpdateBiasedAndFreeAndSurplus_thenCommitLockCanBeAcquired() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);
        orec.___arrive(1);

        boolean result = orec.___tryLockAfterNormalArrive(1, true);

        assertTrue(result);
        assertReadonlyCount(0, orec);
        assertSurplus(2, orec);
        assertUpdateBiased(orec);
        assertHasCommitLock(orec);
        assertHasNoUpdateLock(orec);
    }

    @Test
    public void whenUpdateBiasedAndFreeAndSurplus_thenUpdateLockCanBeAcquired() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);
        orec.___arrive(1);

        boolean result = orec.___tryLockAfterNormalArrive(1, false);

        assertTrue(result);
        assertReadonlyCount(0, orec);
        assertHasUpdateLock(orec);
        assertSurplus(2, orec);
        assertUpdateBiased(orec);
        assertHasNoCommitLock(orec);
    }

    @Test
    public void whenUpdateBiasedAndLockedForCommit_thenCommitLockCantBeAcquired() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);
        //conflicting lock
        orec.___tryLockAfterNormalArrive(1, true);

        boolean result = orec.___tryLockAfterNormalArrive(1, true);

        assertFalse(result);
        assertReadonlyCount(0, orec);
        assertHasCommitLock(orec);
        assertHasNoUpdateLock(orec);
        assertEquals(1, orec.___getSurplus());
        assertFalse(orec.___isReadBiased());
    }

    @Test
    public void whenUpdateBiasedAndLockedForCommit_thenUpdateLockCantBeAcquired() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);
        //conflicting lock
        orec.___tryLockAfterNormalArrive(1, true);

        boolean result = orec.___tryLockAfterNormalArrive(1, false);

        assertFalse(result);
        assertReadonlyCount(0, orec);
        assertHasCommitLock(orec);
        assertHasNoUpdateLock(orec);
        assertEquals(1, orec.___getSurplus());
        assertFalse(orec.___isReadBiased());
    }

    @Test
    public void whenUpdateBiasedAndLockedForUpdate_thenCommitLockCantBeAcquired() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);
        //conflicting lock
        orec.___tryLockAfterNormalArrive(1, false);

        boolean result = orec.___tryLockAfterNormalArrive(1, true);

        assertFalse(result);
        assertReadonlyCount(0, orec);
        assertHasNoCommitLock(orec);
        assertHasUpdateLock(orec);
        assertEquals(1, orec.___getSurplus());
        assertFalse(orec.___isReadBiased());
    }

    @Test
    public void whenUpdateBiasedAndLockedForUpdate_thenUpdateLockCantBeAcquired() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);
        //conflicting lock
        orec.___tryLockAfterNormalArrive(1, false);

        boolean result = orec.___tryLockAfterNormalArrive(1, false);

        assertFalse(result);
        assertReadonlyCount(0, orec);
        assertHasNoCommitLock(orec);
        assertHasUpdateLock(orec);
        assertEquals(1, orec.___getSurplus());
        assertFalse(orec.___isReadBiased());
    }

    @Test
    @Ignore
    public void whenReadBiasedAndNoSurplus_thenPanicErrorWhenCommitLockAcquired() {
        FastOrec orec = makeReadBiased(new FastOrec());

        try {
            orec.___tryLockAfterNormalArrive(1, true);
            fail();
        } catch (PanicError expected) {
        }

        assertReadonlyCount(0, orec);
        assertSurplus(0, orec);
        assertReadBiased(orec);
        assertHasNoUpdateLock(orec);
        assertHasNoCommitLock(orec);
    }

    @Test
    @Ignore
    public void whenReadBiasedAndNoSurplus_thenPanicErrorWhenUpdateLockAcquired() {
        FastOrec orec = makeReadBiased(new FastOrec());

        try {
            orec.___tryLockAfterNormalArrive(1, false);
            fail();
        } catch (PanicError expected) {
        }

        assertReadonlyCount(0, orec);
        assertSurplus(0, orec);
        assertReadBiased(orec);
        assertHasNoUpdateLock(orec);
        assertHasNoCommitLock(orec);
    }

    @Test
    @Ignore
    public void whenReadBiasedAndFree_thenCommitLockCanBeAcquired() {
        FastOrec orec = makeReadBiased(new FastOrec());
        orec.___arrive(1);

        boolean result = orec.___tryLockAfterNormalArrive(1, true);

        assertTrue(result);
        assertReadonlyCount(0, orec);
        assertSurplus(1, orec);
        assertReadBiased(orec);
        assertHasUpdateLock(orec);
        assertHasCommitLock(orec);
    }

    @Test
    @Ignore
    public void whenReadBiasedAndFree_thenUpdateLockCanBeAcquired() {
        FastOrec orec = makeReadBiased(new FastOrec());
        orec.___arrive(1);

        boolean result = orec.___tryLockAfterNormalArrive(1, false);

        assertTrue(result);
        assertReadonlyCount(0, orec);
        assertSurplus(1, orec);
        assertReadBiased(orec);
        assertHasUpdateLock(orec);
        assertHasNoCommitLock(orec);
    }

    @Test
    @Ignore
    public void whenReadBiasedAndLockedForCommit_thenCommitLockCantBeAcquired() {
        FastOrec orec = makeReadBiased(new FastOrec());

        orec.___tryLockAndArrive(1, true);

        boolean result = orec.___tryLockAfterNormalArrive(1, true);

        assertFalse(result);
        assertReadonlyCount(0, orec);
        assertHasCommitLock(orec);
        assertHasUpdateLock(orec);
        assertEquals(1, orec.___getSurplus());
        assertFalse(orec.___isReadBiased());
    }

    @Test
    @Ignore
    public void whenReadBiasedAndLockedForCommit_thenUpdateLockCantBeAcquired() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);
        //conflicting lock
        orec.___tryLockAfterNormalArrive(1, true);

        boolean result = orec.___tryLockAfterNormalArrive(1, false);

        assertFalse(result);
        assertReadonlyCount(0, orec);
        assertHasCommitLock(orec);
        assertHasUpdateLock(orec);
        assertEquals(1, orec.___getSurplus());
        assertFalse(orec.___isReadBiased());
    }

    @Test
    @Ignore
    public void whenReadBiasedAndLockedForUpdate_thenCommitLockCantBeAcquired() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);
        //conflicting lock
        orec.___tryLockAfterNormalArrive(1, false);

        boolean result = orec.___tryLockAfterNormalArrive(1, true);

        assertFalse(result);
        assertReadonlyCount(0, orec);
        assertHasNoCommitLock(orec);
        assertHasUpdateLock(orec);
        assertEquals(1, orec.___getSurplus());
        assertFalse(orec.___isReadBiased());
    }

    @Test
    @Ignore
    public void whenReadBiasedAndLockedForUpdate_thenUpdateLockCantBeAcquired() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);
        //conflicting lock
        orec.___tryLockAfterNormalArrive(1, false);

        boolean result = orec.___tryLockAfterNormalArrive(1, false);

        assertFalse(result);
        assertReadonlyCount(0, orec);
        assertHasNoCommitLock(orec);
        assertHasUpdateLock(orec);
        assertEquals(1, orec.___getSurplus());
        assertFalse(orec.___isReadBiased());
    }

}
