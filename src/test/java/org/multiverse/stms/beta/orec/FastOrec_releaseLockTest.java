package org.multiverse.stms.beta.orec;

import org.junit.Test;
import org.multiverse.api.exceptions.PanicError;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Peter Veentjer
 */
public class FastOrec_releaseLockTest {

    @Test
    public void whenNoSurplusAndNotLocked_thenPanicError() {
        FastOrec orec = new FastOrec();

        try {
            orec.unlockAfterBecomingReadBiased();
            fail();
        } catch (PanicError expected) {
        }

        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertUpdateBiased(orec);
        OrecTestUtils.assertSurplus(0, orec);
    }

    @Test
    public void whenSurplusAndNotLocked_thenPanicError() {
        FastOrec orec = new FastOrec();
        orec.arrive(1);


        try {
            orec.unlockAfterBecomingReadBiased();
            fail();
        } catch (PanicError expected) {
        }

        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertUpdateBiased(orec);
        OrecTestUtils.assertSurplus(1, orec);
    }

    @Test
    public void testReadBiased() {
        FastOrec orec = new FastOrec();
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
        FastOrec orec = new FastOrec();
        orec.arrive(1);
        orec.tryUpdateLock(1);

        orec.unlockAfterBecomingReadBiased();

        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertUpdateBiased(orec);
        OrecTestUtils.assertSurplus(1, orec);
    }
}
