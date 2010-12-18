package org.multiverse.stms.beta.orec;

import org.junit.Test;
import org.multiverse.stms.beta.BetaStmConstants;

import static org.junit.Assert.assertEquals;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class FastOrec_tryLockAndArriveTest implements BetaStmConstants {

    @Test
    public void whenUpdateBiasedAndAlreadyLockedForCommit() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);
        orec.___tryLockAfterNormalArrive(1, true);

        int result = orec.___tryLockAndArrive(1, true);

        assertEquals(ARRIVE_LOCK_NOT_FREE, result);
        assertSurplus(1, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertHasNoUpdateLock(orec);
        assertHasCommitLock(orec);
    }

    @Test
    public void whenUpdateBiasedAndUnlocked() {
        FastOrec orec = new FastOrec();

        int result = orec.___tryLockAndArrive(1, true);

        assertEquals(ARRIVE_NORMAL, result);
        assertSurplus(1, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertHasCommitLock(orec);
        assertHasNoUpdateLock(orec);
    }

    @Test
    public void whenReadBiasedAndLockedForCommit() {
        FastOrec orec = makeReadBiased(new FastOrec());
        orec.___arrive(1);
        orec.___tryLockAfterNormalArrive(1, true);

        int result = orec.___tryLockAndArrive(1, true);

        assertEquals(ARRIVE_LOCK_NOT_FREE, result);
        assertSurplus(1, orec);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
        assertHasCommitLock(orec);
        assertHasNoUpdateLock(orec);
    }

    @Test
    public void whenReadBiasedAndUnlocked() {
        FastOrec orec = makeReadBiased(new FastOrec());

        int result = orec.___tryLockAndArrive(1, true);

        assertEquals(ARRIVE_UNREGISTERED, result);
        assertSurplus(1, orec);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
        assertHasCommitLock(orec);
        assertHasNoUpdateLock(orec);
    }
}