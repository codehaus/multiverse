package org.multiverse.stms.beta.orec;

import org.junit.Test;
import org.multiverse.stms.beta.BetaStmConstants;

import static junit.framework.Assert.assertEquals;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;


/**
 * @author Peter Veentjer
 */
public class FastOrec_arriveTest implements BetaStmConstants{

    @Test
    public void whenUpdateBiasedNotLockedAndNoSurplus() {
        FastOrec orec = new FastOrec();
        int result = orec.___arrive(1);

        assertEquals(ARRIVE_NORMAL, result);
        assertSurplus(1, orec);
        assertUnlocked(orec);
        assertReadonlyCount(0, orec);
        assertUpdateBiased(orec);
        assertNotProtectedAgainstUpdate(orec);
    }

    @Test
    public void whenUpdateBiasedAndNotLockedAndSurplus() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);
        orec.___arrive(1);

        int result = orec.___arrive(1);

        assertEquals(ARRIVE_NORMAL, result);
        assertSurplus(3, orec);
        assertReadonlyCount(0, orec);
        assertUnlocked(orec);
        assertUpdateBiased(orec);
        assertNotProtectedAgainstUpdate(orec);
    }

    @Test
    public void whenUpdateBiasedAndLocked() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);
        orec.___tryLockAfterNormalArrive(1);

        int result = orec.___arrive(1);

        assertEquals(ARRIVE_LOCK_NOT_FREE,result);
        assertSurplus(1, orec);
        assertLocked(orec);
        assertReadonlyCount(0, orec);
        assertUpdateBiased(orec);
        assertNotProtectedAgainstUpdate(orec);
    }

    @Test
    public void whenReadBiasedAndLocked() {
        FastOrec orec = OrecTestUtils.makeReadBiased(new FastOrec());
        orec.___arrive(1);
        orec.___tryLockAfterNormalArrive(1);

        int result = orec.___arrive(1);

        assertEquals(ARRIVE_LOCK_NOT_FREE,result);
        assertSurplus(1, orec);
        assertReadonlyCount(0, orec);
        assertLocked(orec);
        assertReadBiased(orec);
        assertNotProtectedAgainstUpdate(orec);
    }

    @Test
    public void whenReadBiasedAndNoSurplus() {
        FastOrec orec = OrecTestUtils.makeReadBiased(new FastOrec());

        int result = orec.___arrive(1);

        assertEquals(ARRIVE_READBIASED,result);
        assertUnlocked(orec);
        assertSurplus(1, orec);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
        assertNotProtectedAgainstUpdate(orec);
    }

    @Test
    public void whenReadBiasedAndSurplus_thenCallIgnored() {
        FastOrec orec = OrecTestUtils.makeReadBiased(new FastOrec());
        orec.___arrive(1);

        int result = orec.___arrive(1);

        assertEquals(ARRIVE_READBIASED,result);
        assertUnlocked(orec);
        assertSurplus(1, orec);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
        assertNotProtectedAgainstUpdate(orec);
    }
}
