package org.multiverse.stms.beta.orec;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Veentjer
 */
public class OrecTestUtils {

    public static void assertHasUpdateLock(Orec orec) {
        assertTrue(orec.___hasUpdateLock());
    }

    public static void assertHasNoUpdateLock(Orec orec) {
        assertFalse(orec.___hasUpdateLock());
    }

    public static void assertHasCommitLock(Orec orec) {
        assertTrue(orec.___hasCommitLock());
    }

    public static void assertHasNoCommitLock(Orec orec) {
        assertFalse(orec.___hasCommitLock());
    }

    public static void assertSurplus(int expectedSurplus, Orec orec) {
        assertEquals(expectedSurplus, orec.___getSurplus());
    }

    public static void assertReadBiased(Orec orec) {
        assertTrue(orec.___isReadBiased());
    }

    public static void assertUpdateBiased(Orec orec) {
        assertFalse(orec.___isReadBiased());
    }

    public static void assertReadonlyCount(int expectedReadonlyCount, Orec orec) {
        assertEquals(expectedReadonlyCount, orec.___getReadonlyCount());
    }

    public static <O extends Orec> O makeReadBiased(O orec) {
        if (orec.___isReadBiased()) {
            return orec;
        }

        int x = orec.___getReadonlyCount();
        for (int k = x; k < orec.___getReadBiasedThreshold(); k++) {
            orec.___arrive(1);
            orec.___departAfterReading();
        }

        assertReadBiased(orec);
        assertHasNoCommitLock(orec);

        return orec;
    }
}
