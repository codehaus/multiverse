package org.multiverse.stms.beta.refs;

import org.junit.Test;
import org.multiverse.stms.beta.orec.OrecTestUtils;

import static org.junit.Assert.fail;

/**
 * @author Peter Veentjer
 */
public class LongRef_Orec_departAfterFailureTest {

    @Test
    public void whenUpdateBiasedAndNoSurplusAndNotLocked_thenIllegalStateException() {
        Ref orec = new Ref();

        try {
            orec.departAfterFailure();
            fail();
        } catch (IllegalStateException expected) {
        }

        OrecTestUtils.assertSurplus(0, orec);
        OrecTestUtils.assertUpdateBiased(orec);
        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
    }

    @Test
    public void whenUpdateBiasedAndSurplusAndNotLocked() {
        Ref orec = new Ref();
        orec.arrive(1);

        orec.departAfterFailure();

        OrecTestUtils.assertSurplus(0, orec);
        OrecTestUtils.assertUpdateBiased(orec);
        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
    }

    @Test
    public void whenUpdateBiasedAndSurplusAndLocked() {
        Ref orec = new Ref();
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
    public void whenReadBiasedAndLocked_thenIllegalStateException() {
        Ref orec = OrecTestUtils.makeReadBiased(new Ref());

        orec.arrive(1);
        orec.tryUpdateLock(1);

        try {
            orec.departAfterFailure();
            fail();
        } catch (IllegalStateException expected) {
        }

        OrecTestUtils.assertLocked(orec);
        OrecTestUtils.assertSurplus(1, orec);
        OrecTestUtils.assertReadBiased(orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
    }

    @Test
    public void whenReadBiasedAndNotLocked_thenIllegalStateException() {
        Ref orec = OrecTestUtils.makeReadBiased(new Ref());

        try {
            orec.departAfterFailure();
            fail();
        } catch (IllegalStateException expected) {
        }

        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertSurplus(0, orec);
        OrecTestUtils.assertReadBiased(orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
    }
}
