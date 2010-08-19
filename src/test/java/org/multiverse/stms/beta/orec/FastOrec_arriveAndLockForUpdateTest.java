package org.multiverse.stms.beta.orec;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class FastOrec_arriveAndLockForUpdateTest {

    @Test
    public void whenUpdateBiasedLocked() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);
        orec.___tryUpdateLock(1);

        boolean result = orec.___arriveAndLockForUpdate(1);

        assertFalse(result);
        assertLocked(orec);
        assertSurplus(1, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenUpdateBiasedAndUnlocked() {
        FastOrec orec = new FastOrec();

        boolean result = orec.___arriveAndLockForUpdate(1);

        assertTrue(result);
        assertLocked(orec);
        assertSurplus(1, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenReadBiasedAndLocked() {
        FastOrec orec = OrecTestUtils.makeReadBiased(new FastOrec());
        orec.___arrive(1);
        orec.___tryUpdateLock(1);

        boolean result = orec.___arriveAndLockForUpdate(1);

        assertFalse(result);
        assertLocked(orec);
        assertSurplus(1, orec);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenReadBiasedAndUnlocked() {
        FastOrec orec = OrecTestUtils.makeReadBiased(new FastOrec());

        boolean result = orec.___arriveAndLockForUpdate(1);

        assertTrue(result);
        assertLocked(orec);
        assertSurplus(1, orec);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
    }
}
