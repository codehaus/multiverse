package org.multiverse.stms.beta.orec;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Veentjer
 */
public class OrecTestUtils {

    @Test
    public void test(){
        System.out.println(Long.toBinaryString(0x1FFFFFFFFFFFFE00L));
        System.out.printf("%o8s\n",0x1FFFFFFFFFFFFE00L);
    }

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
        for (int k = x; k < orec.___getReadBiasedThreshold() ; k++) {
            orec.___arrive(1);
            orec.___departAfterReading();
        }

        assertReadBiased(orec);
        assertUnlocked(orec);

        return orec;
    }
}
