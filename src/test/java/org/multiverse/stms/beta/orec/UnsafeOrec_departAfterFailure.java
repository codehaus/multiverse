package org.multiverse.stms.beta.orec;

import org.junit.Test;
import org.multiverse.api.exceptions.PanicError;

import static org.junit.Assert.fail;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class UnsafeOrec_departAfterFailure {

    @Test
    public void whenUpdateBiasedAndNoSurplusAndNotLocked_thenPanicError() {
        UnsafeOrec orec = new UnsafeOrec();

        try {
            orec.___departAfterFailure();
            fail();
        } catch (PanicError expected) {
        }

        assertSurplus(0, orec);
        assertUpdateBiased(orec);
        assertUnlocked(orec);

    }

    @Test
    public void whenUpdateBiasedAndSurplusAndNotLocked() {
        UnsafeOrec orec = new UnsafeOrec();
        orec.___arrive(1);

        orec.___departAfterFailure();

        assertSurplus(0, orec);
        assertUpdateBiased(orec);
        assertUnlocked(orec);
    }

    @Test
    public void whenUpdateBiasedAndSurplusAndLocked() {
        UnsafeOrec orec = new UnsafeOrec();
        orec.___arrive(1);
        orec.___arrive(1);
        orec.___tryUpdateLock(1);

        orec.___departAfterFailure();

        assertSurplus(1, orec);
        assertUpdateBiased(orec);
        assertLocked(orec);
    }

    //todo: a lot more combinations need to be tested
}
