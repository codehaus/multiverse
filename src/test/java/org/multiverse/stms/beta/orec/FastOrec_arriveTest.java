package org.multiverse.stms.beta.orec;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * @author Peter Veentjer
 */
public class FastOrec_arriveTest {

    @Test
    public void whenUpdateBiasedNotLockedAndNoSurplus() {
        FastOrec orec = new FastOrec();
        boolean result = orec.___arrive(1);

        assertTrue(result);
        OrecTestUtils.assertSurplus(1, orec);
        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
        OrecTestUtils.assertUpdateBiased(orec);
    }

    @Test
    public void whenUpdateBiasedAndNotLockedAndSurplus() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);
        orec.___arrive(1);

        boolean result = orec.___arrive(1);

        assertTrue(result);
        OrecTestUtils.assertSurplus(3, orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertUpdateBiased(orec);
    }

    @Test
    public void whenUpdateBiasedAndLocked_thenReturnFalse() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);
        orec.___tryUpdateLock(1);

        boolean result = orec.___arrive(1);

        assertFalse(result);
        OrecTestUtils.assertSurplus(1, orec);
        OrecTestUtils.assertLocked(orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
        OrecTestUtils.assertUpdateBiased(orec);
    }

    @Test
    public void whenReadBiasedAndLocked_thenReturnFalse() {
        FastOrec orec = OrecTestUtils.makeReadBiased(new FastOrec());
        orec.___arrive(1);
        orec.___tryUpdateLock(1);

        boolean result = orec.___arrive(1);

        assertFalse(result);
        OrecTestUtils.assertSurplus(1, orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
        OrecTestUtils.assertLocked(orec);
        OrecTestUtils.assertReadBiased(orec);
    }

    @Test
    public void whenReadBiasedAndNoSurplus() {
        FastOrec orec = OrecTestUtils.makeReadBiased(new FastOrec());

        boolean result = orec.___arrive(1);

        assertTrue(result);
        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertSurplus(1, orec);
        OrecTestUtils.assertReadBiased(orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
    }

    @Test
    public void whenReadBiasedAndSurplus_thenCallIgnored() {
        FastOrec orec = OrecTestUtils.makeReadBiased(new FastOrec());
        orec.___arrive(1);

        boolean result = orec.___arrive(1);

        assertTrue(result);
        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertSurplus(1, orec);
        OrecTestUtils.assertReadBiased(orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
    }
}
