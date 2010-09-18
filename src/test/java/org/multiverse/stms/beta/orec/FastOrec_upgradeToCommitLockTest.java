package org.multiverse.stms.beta.orec;

import org.junit.Test;
import org.multiverse.api.exceptions.PanicError;

import static org.junit.Assert.fail;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class FastOrec_upgradeToCommitLockTest {

    @Test
    public void whenUpdateBiasedAlreadyUpgradedToCommitLock() {
        FastOrec orec = new FastOrec();
        orec.___tryLockAndArrive(1, true);

        orec.___upgradeToCommitLock();

        assertHasUpdateLock(orec);
        assertHasCommitLock(orec);
        assertSurplus(1, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenUpdateBiasedUpdateLockAlreadyAcquired() {
        FastOrec orec = new FastOrec();
        orec.___tryLockAndArrive(1, false);

        orec.___upgradeToCommitLock();

        assertHasUpdateLock(orec);
        assertHasCommitLock(orec);
        assertSurplus(1, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenMultipleSurplusAndUpdateBiasedAlreadyUpgradedToCommitLock() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);
        orec.___arrive(1);
        orec.___tryLockAndArrive(1, true);

        orec.___upgradeToCommitLock();

        assertHasUpdateLock(orec);
        assertHasCommitLock(orec);
        assertSurplus(3, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenMultipleSurplusAndUpdateBiasedUpdateLockAlreadyAcquired() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);
        orec.___arrive(1);
        orec.___tryLockAndArrive(1, false);

        orec.___upgradeToCommitLock();

        assertHasUpdateLock(orec);
        assertHasCommitLock(orec);
        assertSurplus(3, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenSurplusAndUpdateBiasedNoLockAcquired_thenPanicError() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);

        try {
            orec.___upgradeToCommitLock();
            fail();
        } catch (PanicError expected) {

        }

        assertHasNoUpdateLock(orec);
        assertHasNoCommitLock(orec);
        assertSurplus(1, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenNoSurplusAndUpdateBiasedNoLockAcquired_thenPanicError() {
        FastOrec orec = new FastOrec();

        try {
            orec.___upgradeToCommitLock();
            fail();
        } catch (PanicError expected) {

        }

        assertHasNoUpdateLock(orec);
        assertHasNoCommitLock(orec);
        assertSurplus(0, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenReadBiasedAndNoLocks_thenPanicError() {
        FastOrec orec = makeReadBiased(new FastOrec());

        try {
            orec.___upgradeToCommitLock();
            fail();
        } catch (PanicError expected) {
        }

        assertHasNoUpdateLock(orec);
        assertHasNoCommitLock(orec);
        assertSurplus(0, orec);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenReadBiasedAndAlreadyCommitLock() {
        FastOrec orec = makeReadBiased(new FastOrec());
        orec.___tryLockAndArrive(1, true);

        orec.___upgradeToCommitLock();

        assertHasUpdateLock(orec);
        assertHasCommitLock(orec);
        assertSurplus(1, orec);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenReadBiasedAndAlreadyUpdateLock() {
        FastOrec orec = makeReadBiased(new FastOrec());
        orec.___tryLockAndArrive(1, false);

        orec.___upgradeToCommitLock();

        assertHasUpdateLock(orec);
        assertHasCommitLock(orec);
        assertSurplus(1, orec);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
    }
}
