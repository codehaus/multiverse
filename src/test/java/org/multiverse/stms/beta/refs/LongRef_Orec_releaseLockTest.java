package org.multiverse.stms.beta.refs;

import org.junit.Test;
import org.multiverse.stms.beta.orec.OrecTestUtils;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Peter Veentjer
 */
public class LongRef_Orec_releaseLockTest {

    @Test
    public void whenNoSurplusAndNotLocked_thenIllegalStateException() {
        Ref orec = new Ref();

        try {
            orec.unlockAfterBecomingReadBiased();
            fail();
        } catch (IllegalStateException expected) {
        }

        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertUpdateBiased(orec);
        OrecTestUtils.assertSurplus(0, orec);
    }

    @Test
    public void whenSurplusAndNotLocked_thenIllegalStateException() {
        Ref orec = new Ref();
        orec.arrive(1);


        try {
            orec.unlockAfterBecomingReadBiased();
            fail();
        } catch (IllegalStateException expected) {
        }

        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertUpdateBiased(orec);
        OrecTestUtils.assertSurplus(1, orec);
    }

    @Test
    public void testReadBiased() {
        Ref orec = new Ref();
        for (int k = 0; k < orec.getReadBiasedThreshold() - 1; k++) {
            orec.arrive(1);
            orec.departAfterReading();
            OrecTestUtils.assertReadonlyCount(k + 1, orec);
        }

        orec.arrive(1);
        boolean result = orec.departAfterReading();
        assertTrue(result);

        orec.unlockAfterBecomingReadBiased();
        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertReadBiased(orec);
        OrecTestUtils.assertSurplus(0, orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
    }

    @Test
    public void whenLocked() {
        Ref orec = new Ref();
        orec.arrive(1);
        orec.tryUpdateLock(1);

        orec.unlockAfterBecomingReadBiased();

        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertUpdateBiased(orec);
        OrecTestUtils.assertSurplus(1, orec);
    }
}
