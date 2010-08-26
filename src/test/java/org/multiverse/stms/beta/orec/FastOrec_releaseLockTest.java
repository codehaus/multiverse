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
            orec.___releaseLockAfterBecomingReadBiased();
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
        orec.___arrive(1);


        try {
            orec.___releaseLockAfterBecomingReadBiased();
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
        for (int k = 0; k < orec.___getReadBiasedThreshold() - 1; k++) {
            orec.___arrive(1);
            orec.___departAfterReading();
            OrecTestUtils.assertReadonlyCount(k + 1, orec);
        }

        orec.___arrive(1);
        boolean result = orec.___departAfterReading();
        assertTrue(result);

        orec.___releaseLockAfterBecomingReadBiased();
        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertReadBiased(orec);
        OrecTestUtils.assertSurplus(0, orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
    }

    @Test
    public void whenLocked() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);
        orec.___tryLockAfterArrive(1);

        orec.___releaseLockAfterBecomingReadBiased();

        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertUpdateBiased(orec);
        OrecTestUtils.assertSurplus(1, orec);
    }
}
