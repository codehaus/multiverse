package org.multiverse.stms.beta.refs;

import org.junit.Test;
import org.multiverse.stms.beta.orec.OrecTestUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class LongRef_Orec_arriveAndLockForUpdateTest {

    @Test
    public void whenUpdateBiasedLocked() {
        Ref orec = new Ref();
        orec.arrive(1);
        orec.tryUpdateLock(1);

        boolean result = orec.arriveAndLockForUpdate(1);

        assertFalse(result);
        assertLocked(orec);
        assertSurplus(1, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenUpdateBiasedAndUnlocked() {
        Ref orec = new Ref();

        boolean result = orec.arriveAndLockForUpdate(1);

        assertTrue(result);
        assertLocked(orec);
        assertSurplus(1, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenReadBiasedAndLocked() {
        Ref orec = OrecTestUtils.makeReadBiased(new Ref());
        orec.arrive(1);
        orec.tryUpdateLock(1);

        boolean result = orec.arriveAndLockForUpdate(1);

        assertFalse(result);
        assertLocked(orec);
        assertSurplus(1, orec);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenReadBiasedAndUnlocked() {
        Ref orec = OrecTestUtils.makeReadBiased(new Ref());

        boolean result = orec.arriveAndLockForUpdate(1);

        assertTrue(result);
        assertLocked(orec);
        assertSurplus(1, orec);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
    }
}
