package org.multiverse.stms.beta.refs;

import org.junit.Test;
import org.multiverse.stms.beta.orec.OrecTestUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * @author Peter Veentjer
 */
public class LongRef_Orec_arriveTest {

    @Test
    public void whenUpdateBiasedNotLockedAndNoSurplus() {
        Ref orec = new Ref();
        boolean result = orec.arrive(1);

        assertTrue(result);
        OrecTestUtils.assertSurplus(1, orec);
        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
        OrecTestUtils.assertUpdateBiased(orec);
    }

    @Test
    public void whenUpdateBiasedAndNotLockedAndSurplus() {
        Ref orec = new Ref();
        orec.arrive(1);
        orec.arrive(1);

        boolean result = orec.arrive(1);

        assertTrue(result);
        OrecTestUtils.assertSurplus(3, orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertUpdateBiased(orec);
    }

    @Test
    public void whenUpdateBiasedAndLocked_thenReturnFalse() {
        Ref orec = new Ref();
        orec.arrive(1);
        orec.tryUpdateLock(1);

        boolean result = orec.arrive(1);

        assertFalse(result);
        OrecTestUtils.assertSurplus(1, orec);
        OrecTestUtils.assertLocked(orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
        OrecTestUtils.assertUpdateBiased(orec);
    }

    @Test
    public void whenReadBiasedAndLocked_thenReturnFalse() {
        Ref orec = OrecTestUtils.makeReadBiased(new Ref());
        orec.arrive(1);
        orec.tryUpdateLock(1);

        boolean result = orec.arrive(1);

        assertFalse(result);
        OrecTestUtils.assertSurplus(1, orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
        OrecTestUtils.assertLocked(orec);
        OrecTestUtils.assertReadBiased(orec);
    }

    @Test
    public void whenReadBiasedAndNoSurplus() {
        Ref orec = OrecTestUtils.makeReadBiased(new Ref());

        boolean result = orec.arrive(1);

        assertTrue(result);
        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertSurplus(1, orec);
        OrecTestUtils.assertReadBiased(orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
    }

    @Test
    public void whenReadBiasedAndSurplus_thenCallIgnored() {
        Ref orec = OrecTestUtils.makeReadBiased(new Ref());
        orec.arrive(1);

        boolean result = orec.arrive(1);

        assertTrue(result);
        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertSurplus(1, orec);
        OrecTestUtils.assertReadBiased(orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
    }
}
