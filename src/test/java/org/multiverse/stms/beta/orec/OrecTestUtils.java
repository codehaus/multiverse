package org.multiverse.stms.beta.orec;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;

/**
 * @author Peter Veentjer
 */
public class OrecTestUtils {

    public static void assertLocked(Orec orec) {
        assertTrue(orec.___isLocked());
    }

    public static void assertUnlocked(Orec orec) {
        assertFalse(orec.___isLocked());
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
        for (int k = x; k < orec.___getReadBiasedThreshold() - 1; k++) {
            orec.___arrive(1);
            if (orec.___departAfterReading()) {
                throw new RuntimeException();
            }
        }

        orec.___arrive(1);
        if (orec.___departAfterReading()) {
            orec.___unlockAfterBecomingReadBiased();
        } else {
            fail();
        }

        assertReadBiased(orec);
        assertUnlocked(orec);

        return orec;
    }
}
