package org.multiverse.stms.beta.refs;

import org.junit.Test;
import org.multiverse.stms.beta.orec.OrecTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Peter Veentjer
 */
public class LongRef_Orec_departAfterFailureAndReleaseLockTest {

    @Test
    public void whenUpdateBiasedNotLocked_thenIllegalStateException() {
        Ref orec = new Ref();
        try {
            orec.departAfterFailureAndReleaseLock();
            fail();
        } catch (IllegalStateException ex) {
        }

        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertSurplus(0, orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
        OrecTestUtils.assertUpdateBiased(orec);
    }

    @Test
    public void whenReadBiasedAndNotLocked_thenIllegalStateException() {
        Ref orec = OrecTestUtils.makeReadBiased(new Ref());

        try {
            orec.departAfterFailureAndReleaseLock();
            fail();
        } catch (IllegalStateException expected) {
        }

        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertSurplus(0, orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
        OrecTestUtils.assertReadBiased(orec);
    }

    @Test
    public void whenUpdateBiasedAndLocked() {
        Ref orec = new Ref();
        orec.arrive(1);
        orec.tryUpdateLock(1);

        long result = orec.departAfterFailureAndReleaseLock();
        assertEquals(0, result);
        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertSurplus(0, orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
        OrecTestUtils.assertUpdateBiased(orec);
    }

    @Test
    public void whenReadBiasedAndLocked() {
        Ref orec = OrecTestUtils.makeReadBiased(new Ref());
        orec.arrive(1);
        orec.tryUpdateLock(1);

        long result = orec.departAfterFailureAndReleaseLock();
        assertEquals(1, result);
        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertSurplus(1, orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
        OrecTestUtils.assertReadBiased(orec);
    }
}
