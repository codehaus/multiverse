package org.multiverse.stms.beta.orec;

import org.junit.Test;
import org.multiverse.api.exceptions.PanicError;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class UnsafeOrec_departAfterReadingTest {

    @Test
    public void whenNoSurplus_thenPanicError() {
        UnsafeOrec orec = new UnsafeOrec();

        try {
            orec.___departAfterReading();
            fail();
        } catch (PanicError expected) {
        }
    }

    @Test
    public void whenMuchSurplus() {
        UnsafeOrec orec = new UnsafeOrec();
        orec.___arrive(1);
        orec.___arrive(1);

        boolean result = orec.___departAfterReading();
        assertFalse(result);
        assertSurplus(1, orec);
        assertUnlocked(orec);
    }

    @Test
    public void whenLocked() {
        UnsafeOrec orec = new UnsafeOrec();
        orec.___arrive(1);
        orec.___arrive(1);
        orec.___tryUpdateLock(1);

        boolean result = orec.___departAfterReading();
        assertFalse(result);
        assertLocked(orec);
        assertSurplus(1, orec);
        assertFalse(orec.___isReadBiased());
    }

    @Test
    public void whenReadBiased_thenPanicError() {
        UnsafeOrec orec = makeReadBiased(new UnsafeOrec());

        try {
            orec.___departAfterReading();
            fail();
        } catch (PanicError expected) {
        }

        assertUnlocked(orec);
        assertSurplus(0, orec);
        assertReadBiased(orec);
    }
}
