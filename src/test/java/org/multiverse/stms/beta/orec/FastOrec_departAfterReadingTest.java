package org.multiverse.stms.beta.orec;

import org.junit.Test;
import org.multiverse.api.exceptions.PanicError;

import static org.junit.Assert.fail;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

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

        assertHasNoCommitLock(orec);
        assertSurplus(0, orec);
        assertReadonlyCount(0, orec);
        assertUpdateBiased(orec);
        assertHasNoUpdateLock(orec);
    }

    @Test
    public void whenMuchSurplus() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);
        orec.___arrive(1);

        orec.___departAfterReading();

        assertSurplus(1, orec);
        assertHasNoCommitLock(orec);
        assertReadonlyCount(1, orec);
        assertUpdateBiased(orec);
        assertHasNoUpdateLock(orec);
    }

    @Test
    public void whenLocked() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);
        orec.___arrive(1);
        orec.___tryLockAfterNormalArrive(1,true);

        orec.___departAfterReading();

        assertHasCommitLock(orec);
        assertSurplus(1, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(1, orec);
        assertHasNoUpdateLock(orec);
    }

    @Test
    public void whenReadBiasedAndLocked_thenPanicError() {
        FastOrec orec = makeReadBiased(new FastOrec());
        orec.___arrive(1);
        orec.___tryLockAfterNormalArrive(1,true);

        try {
            orec.___departAfterReading();
            fail();
        } catch (PanicError expected) {

        }

        assertHasCommitLock(orec);
        assertSurplus(1, orec);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
        assertHasNoUpdateLock(orec);
    }

    @Test
    public void whenReadBiasedAndUnlocked_thenPanicError() {
        FastOrec orec = makeReadBiased(new FastOrec());

        try {
            orec.___departAfterReading();
            fail();
        } catch (PanicError expected) {

        }

        assertHasNoCommitLock(orec);
        assertSurplus(0, orec);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
        assertHasNoUpdateLock(orec);
    }
}
