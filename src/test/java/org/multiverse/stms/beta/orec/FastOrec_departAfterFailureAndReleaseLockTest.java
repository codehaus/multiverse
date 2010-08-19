package org.multiverse.stms.beta.orec;

import org.junit.Test;
import org.multiverse.api.exceptions.PanicError;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Peter Veentjer
 */
public class FastOrec_departAfterFailureAndReleaseLockTest {

    @Test
    public void whenUpdateBiasedNotLocked_thenPanicError() {
        FastOrec orec = new FastOrec();
        try {
            orec.___departAfterFailureAndReleaseLock();
            fail();
        } catch (PanicError ex) {
        }

        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertSurplus(0, orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
        OrecTestUtils.assertUpdateBiased(orec);
    }

    @Test
    public void whenReadBiasedAndNotLocked_thenPanicError() {
        Orec orec = OrecTestUtils.makeReadBiased(new FastOrec());

        try {
            orec.___departAfterFailureAndReleaseLock();
            fail();
        } catch (PanicError expected) {
        }

        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertSurplus(0, orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
        OrecTestUtils.assertReadBiased(orec);
    }

    @Test
    public void whenUpdateBiasedAndLocked() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);
        orec.___tryUpdateLock(1);

        long result = orec.___departAfterFailureAndReleaseLock();
        assertEquals(0, result);
        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertSurplus(0, orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
        OrecTestUtils.assertUpdateBiased(orec);
    }

    @Test
    public void whenReadBiasedAndLocked() {
        FastOrec orec = OrecTestUtils.makeReadBiased(new FastOrec());
        orec.___arrive(1);
        orec.___tryUpdateLock(1);

        long result = orec.___departAfterFailureAndReleaseLock();
        assertEquals(1, result);
        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertSurplus(1, orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
        OrecTestUtils.assertReadBiased(orec);
    }
}
