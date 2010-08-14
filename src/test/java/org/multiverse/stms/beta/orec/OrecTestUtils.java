package org.multiverse.stms.beta.orec;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;

/**
 * @author Peter Veentjer
 */
public class OrecTestUtils {

    public static void assertLocked(Orec orec) {
        assertTrue(orec.isLocked());
    }

    public static void assertUnlocked(Orec orec) {
        assertFalse(orec.isLocked());
    }

    public static void assertSurplus(int expectedSurplus, Orec orec) {
        assertEquals(expectedSurplus, orec.getSurplus());
    }

    public static void assertReadBiased(Orec orec) {
        assertTrue(orec.isReadBiased());
    }

    public static void assertUpdateBiased(Orec orec) {
        assertFalse(orec.isReadBiased());
    }


    public static void assertReadonlyCount(int expectedReadonlyCount, Orec orec) {
        assertEquals(expectedReadonlyCount, orec.getReadonlyCount());
    }

    public static <O extends Orec> O makeReadBiased(O orec) {
        if (orec.isReadBiased()) {
            return orec;
        }

        int x = orec.getReadonlyCount();
        for (int k = x; k < orec.getReadBiasedThreshold() - 1; k++) {
            orec.arrive(1);
            if (orec.departAfterReading()) {
                throw new RuntimeException();
            }
        }

        orec.arrive(1);
        if (orec.departAfterReading()) {
            orec.unlockAfterBecomingReadBiased();
        } else {
            fail();
        }

        assertReadBiased(orec);
        assertUnlocked(orec);

        return orec;
    }
}
