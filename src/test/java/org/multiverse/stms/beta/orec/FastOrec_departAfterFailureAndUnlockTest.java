package org.multiverse.stms.beta.orec;

import org.junit.Test;
import org.multiverse.api.exceptions.PanicError;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class FastOrec_departAfterFailureAndUnlockTest {

    @Test
    public void whenUpdateBiasedNotLocked_thenPanicError() {
        FastOrec orec = new FastOrec();
        try {
            orec.___departAfterFailureAndUnlock();
            fail();
        } catch (PanicError ex) {
        }

        assertUnlocked(orec);
        assertSurplus(0, orec);
        assertReadonlyCount(0, orec);
        assertUpdateBiased(orec);
        assertNotProtectedAgainstUpdate(orec);
    }

    @Test
    public void whenReadBiasedAndNotLocked_thenPanicError() {
        Orec orec = OrecTestUtils.makeReadBiased(new FastOrec());

        try {
            orec.___departAfterFailureAndUnlock();
            fail();
        } catch (PanicError expected) {
        }

        assertUnlocked(orec);
        assertSurplus(0, orec);
        assertReadonlyCount(0, orec);
        assertReadBiased(orec);
        assertNotProtectedAgainstUpdate(orec);
    }

    @Test
    public void whenUpdateBiasedAndLocked() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);
        orec.___tryLockAfterNormalArrive(1);

        long result = orec.___departAfterFailureAndUnlock();
        assertEquals(0, result);
        assertUnlocked(orec);
        assertSurplus(0, orec);
        assertReadonlyCount(0, orec);
        assertUpdateBiased(orec);
        assertNotProtectedAgainstUpdate(orec);
    }

    @Test
    public void whenReadBiasedAndLocked() {
        FastOrec orec = OrecTestUtils.makeReadBiased(new FastOrec());
        orec.___arrive(1);
        orec.___tryLockAfterNormalArrive(1);

        long result = orec.___departAfterFailureAndUnlock();
        assertEquals(1, result);
        assertUnlocked(orec);
        assertSurplus(1, orec);
        assertReadonlyCount(0, orec);
        assertReadBiased(orec);
        assertNotProtectedAgainstUpdate(orec);
    }
}
