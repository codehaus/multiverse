package org.multiverse.stms.beta.transactionalobjects;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Veentjer
 */
public class OrecTestUtils {

    public static void assertHasUpdateLock(BetaTransactionalObject orec) {
        assertTrue(orec.___hasUpdateLock());
    }

    public static void assertHasNoUpdateLock(BetaTransactionalObject orec) {
        assertFalse(orec.___hasUpdateLock());
    }

    public static void assertHasCommitLock(BetaTransactionalObject orec) {
        assertTrue(orec.___hasCommitLock());
    }

    public static void assertHasNoCommitLock(BetaTransactionalObject orec) {
        assertFalse(orec.___hasCommitLock());
    }

    public static void assertSurplus(int expectedSurplus, BetaTransactionalObject orec) {
        assertEquals(expectedSurplus, orec.___getSurplus());
    }

    public static void assertReadBiased(BetaTransactionalObject orec) {
        assertTrue(orec.___isReadBiased());
    }

    public static void assertUpdateBiased(BetaTransactionalObject orec) {
        assertFalse(orec.___isReadBiased());
    }

    public static void assertReadonlyCount(int expectedReadonlyCount, BetaTransactionalObject orec) {
        assertEquals(expectedReadonlyCount, orec.___getReadonlyCount());
    }

    public static <O extends BetaTransactionalObject> O makeReadBiased(O orec) {
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
