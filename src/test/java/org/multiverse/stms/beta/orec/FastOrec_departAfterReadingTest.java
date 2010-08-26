package org.multiverse.stms.beta.orec;

import org.junit.Test;
import org.multiverse.api.exceptions.PanicError;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * @author Peter Veentjer
 */
public class FastOrec_departAfterReadingTest {

    @Test
    public void whenNoSurplus_thenPanicError() {
        FastOrec orec = new FastOrec();

        try {
            orec.___departAfterReading();
            fail();
        } catch (PanicError expected) {
        }

        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertSurplus(0, orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
        OrecTestUtils.assertUpdateBiased(orec);
    }

    @Test
    public void whenMuchSurplus() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);
        orec.___arrive(1);

        boolean result = orec.___departAfterReading();

        assertFalse(result);
        OrecTestUtils.assertSurplus(1, orec);
        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertReadonlyCount(1, orec);
        OrecTestUtils.assertUpdateBiased(orec);
    }

    @Test
    public void whenLocked() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);
        orec.___arrive(1);
        orec.___tryLockAfterArrive(1);

        boolean result = orec.___departAfterReading();

        assertFalse(result);
        OrecTestUtils.assertLocked(orec);
        OrecTestUtils.assertSurplus(1, orec);
        OrecTestUtils.assertUpdateBiased(orec);
        OrecTestUtils.assertReadonlyCount(1, orec);
    }

    @Test
    public void whenReadBiasedAndLocked_thenPanicError() {
        FastOrec orec = OrecTestUtils.makeReadBiased(new FastOrec());
        orec.___arrive(1);
        orec.___tryLockAfterArrive(1);

        try {
            orec.___departAfterReading();
            fail();
        } catch (PanicError expected) {

        }

        OrecTestUtils.assertLocked(orec);
        OrecTestUtils.assertSurplus(1, orec);
        OrecTestUtils.assertReadBiased(orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
    }

    @Test
    public void whenReadBiasedAndUnlocked_thenPanicError() {
        FastOrec orec = OrecTestUtils.makeReadBiased(new FastOrec());

        try {
            orec.___departAfterReading();
            fail();
        } catch (PanicError expected) {

        }

        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertSurplus(0, orec);
        OrecTestUtils.assertReadBiased(orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
    }
}
