package org.multiverse.stms.beta.orec;

import org.junit.Test;

import static org.junit.Assert.fail;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class UnsafeOrec_departAfterFailure {

    @Test
    public void whenUpdateBiasedAndNoSurplusAndNotLocked_thenIllegalStateException() {
        UnsafeOrec orec = new UnsafeOrec();

        try {
            orec.departAfterFailure();
            fail();
        } catch (IllegalStateException expected) {
        }

        assertSurplus(0, orec);
        assertUpdateBiased(orec);
        assertUnlocked(orec);

    }

    @Test
    public void whenUpdateBiasedAndSurplusAndNotLocked() {
        UnsafeOrec orec = new UnsafeOrec();
        orec.arrive(1);

        orec.departAfterFailure();

        assertSurplus(0, orec);
        assertUpdateBiased(orec);
        assertUnlocked(orec);
    }

    @Test
    public void whenUpdateBiasedAndSurplusAndLocked() {
        UnsafeOrec orec = new UnsafeOrec();
        orec.arrive(1);
        orec.arrive(1);
        orec.tryUpdateLock(1);

        orec.departAfterFailure();

        assertSurplus(1, orec);
        assertUpdateBiased(orec);
        assertLocked(orec);
    }

    //todo: a lot more combinations need to be tested
}
