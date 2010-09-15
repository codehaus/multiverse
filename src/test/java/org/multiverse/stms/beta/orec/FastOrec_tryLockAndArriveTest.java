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
    public void whenUpdateBiasedAndAlreadyLocked() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);
        orec.___tryLockAfterNormalArrive(1);

        int result = orec.___tryLockAndArrive(1);

        assertEquals(ARRIVE_LOCK_NOT_FREE, result);
        assertLocked(orec);
        assertSurplus(1, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertNotProtectedAgainstUpdate(orec);
    }

    @Test
    public void whenUpdateBiasedAndUnlocked() {
        FastOrec orec = new FastOrec();

        int result = orec.___tryLockAndArrive(1);

        assertEquals(ARRIVE_NORMAL, result);
        assertLocked(orec);
        assertSurplus(1, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertNotProtectedAgainstUpdate(orec);
    }

    @Test
    public void whenReadBiasedAndLocked() {
        FastOrec orec = OrecTestUtils.makeReadBiased(new FastOrec());
        orec.___arrive(1);
        orec.___tryLockAfterNormalArrive(1);

        int result = orec.___tryLockAndArrive(1);

        assertEquals(ARRIVE_LOCK_NOT_FREE, result);
        assertLocked(orec);
        assertSurplus(1, orec);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
        assertNotProtectedAgainstUpdate(orec);
    }

    @Test
    public void whenReadBiasedAndUnlocked() {
        FastOrec orec = OrecTestUtils.makeReadBiased(new FastOrec());

        int result = orec.___tryLockAndArrive(1);

        assertEquals(ARRIVE_READBIASED, result);
        assertLocked(orec);
        assertSurplus(1, orec);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
        assertNotProtectedAgainstUpdate(orec);
    }
}
