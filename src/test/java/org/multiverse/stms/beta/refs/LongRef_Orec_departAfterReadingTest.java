package org.multiverse.stms.beta.refs;

import org.junit.Test;
import org.multiverse.stms.beta.orec.OrecTestUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * @author Peter Veentjer
 */
public class LongRef_Orec_departAfterReadingTest {

    @Test
    public void whenNoSurplus() {
        Ref orec = new Ref();

        try {
            orec.departAfterReading();
            fail();
        } catch (IllegalStateException expected) {
        }

        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertSurplus(0, orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
        OrecTestUtils.assertUpdateBiased(orec);
    }

    @Test
    public void whenMuchSurplus() {
        Ref orec = new Ref();
        orec.arrive(1);
        orec.arrive(1);

        boolean result = orec.departAfterReading();

        assertFalse(result);
        OrecTestUtils.assertSurplus(1, orec);
        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertReadonlyCount(1, orec);
        OrecTestUtils.assertUpdateBiased(orec);
    }

    @Test
    public void whenLocked() {
        Ref orec = new Ref();
        orec.arrive(1);
        orec.arrive(1);
        orec.tryUpdateLock(1);

        boolean result = orec.departAfterReading();

        assertFalse(result);
        OrecTestUtils.assertLocked(orec);
        OrecTestUtils.assertSurplus(1, orec);
        OrecTestUtils.assertUpdateBiased(orec);
        OrecTestUtils.assertReadonlyCount(1, orec);
    }

    @Test
    public void whenReadBiasedAndLocked_thenIllegalStateException() {
        Ref orec = OrecTestUtils.makeReadBiased(new Ref());
        orec.arrive(1);
        orec.tryUpdateLock(1);

        try {
            orec.departAfterReading();
            fail();
        } catch (IllegalStateException expected) {

        }

        OrecTestUtils.assertLocked(orec);
        OrecTestUtils.assertSurplus(1, orec);
        OrecTestUtils.assertReadBiased(orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
    }

    @Test
    public void whenReadBiasedAndUnlocked_thenIllegalStateException() {
        Ref orec = OrecTestUtils.makeReadBiased(new Ref());

        try {
            orec.departAfterReading();
            fail();
        } catch (IllegalStateException expected) {

        }

        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertSurplus(0, orec);
        OrecTestUtils.assertReadBiased(orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
    }
}
