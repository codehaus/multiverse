package org.multiverse.stms.beta.orec;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class UnsafeOrec_departAfterReadingTest {

    @Test
    public void whenNoSurplus() {
        UnsafeOrec orec = new UnsafeOrec();

        try {
            orec.departAfterReading();
            fail();
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void whenMuchSurplus() {
        UnsafeOrec orec = new UnsafeOrec();
        orec.arrive(1);
        orec.arrive(1);

        boolean result = orec.departAfterReading();
        assertFalse(result);
        assertSurplus(1, orec);
        assertUnlocked(orec);
    }

    @Test
    public void whenLocked() {
        UnsafeOrec orec = new UnsafeOrec();
        orec.arrive(1);
        orec.arrive(1);
        orec.tryUpdateLock(1);

        boolean result = orec.departAfterReading();
        assertFalse(result);
        assertLocked(orec);
        assertSurplus(1, orec);
        assertFalse(orec.isReadBiased());
    }

    @Test
    public void whenReadBiased_thenIllegalStateException() {
        UnsafeOrec orec = makeReadBiased(new UnsafeOrec());

        try {
            orec.departAfterReading();
            fail();
        } catch (IllegalStateException expected) {

        }

        assertUnlocked(orec);
        assertSurplus(0, orec);
        assertReadBiased(orec);
    }
}
