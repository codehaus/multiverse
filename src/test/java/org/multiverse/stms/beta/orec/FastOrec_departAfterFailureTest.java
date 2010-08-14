package org.multiverse.stms.beta.orec;

import org.junit.Test;
import org.multiverse.api.exceptions.PanicError;

import static org.junit.Assert.fail;

/**
 * @author Peter Veentjer
 */
public class FastOrec_departAfterFailureTest {

    @Test
    public void whenUpdateBiasedAndNoSurplusAndNotLocked_thenPanicError() {
        FastOrec orec = new FastOrec();

        try {
            orec.departAfterFailure();
            fail();
        } catch (PanicError expected) {
        }

        OrecTestUtils.assertSurplus(0, orec);
        OrecTestUtils.assertUpdateBiased(orec);
        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
    }

    @Test
    public void whenUpdateBiasedAndSurplusAndNotLocked() {
        FastOrec orec = new FastOrec();
        orec.arrive(1);

        orec.departAfterFailure();

        OrecTestUtils.assertSurplus(0, orec);
        OrecTestUtils.assertUpdateBiased(orec);
        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
    }

    @Test
    public void whenUpdateBiasedAndSurplusAndLocked() {
        FastOrec orec = new FastOrec();
        orec.arrive(1);
        orec.arrive(1);
        orec.tryUpdateLock(1);

        orec.departAfterFailure();

        OrecTestUtils.assertSurplus(1, orec);
        OrecTestUtils.assertUpdateBiased(orec);
        OrecTestUtils.assertLocked(orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
    }

    @Test
    public void whenReadBiasedAndLocked_thenPanicError() {
        FastOrec orec = OrecTestUtils.makeReadBiased(new FastOrec());

        orec.arrive(1);
        orec.tryUpdateLock(1);

        try {
            orec.departAfterFailure();
            fail();
        } catch (PanicError expected) {
        }

        OrecTestUtils.assertLocked(orec);
        OrecTestUtils.assertSurplus(1, orec);
        OrecTestUtils.assertReadBiased(orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
    }

    @Test
    public void whenReadBiasedAndNotLocked_thenPanicError() {
        FastOrec orec = OrecTestUtils.makeReadBiased(new FastOrec());

        try {
            orec.departAfterFailure();
            fail();
        } catch (PanicError expected) {
        }

        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertSurplus(0, orec);
        OrecTestUtils.assertReadBiased(orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
    }
}
