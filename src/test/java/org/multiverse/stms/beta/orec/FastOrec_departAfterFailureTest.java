package org.multiverse.stms.beta.orec;

import org.junit.Test;
import org.multiverse.api.exceptions.PanicError;

import static org.junit.Assert.fail;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class FastOrec_departAfterFailureTest {

    @Test
    public void whenUpdateBiasedAndNoSurplusAndNotLocked_thenPanicError() {
        FastOrec orec = new FastOrec();

        try {
            orec.___departAfterFailure();
            fail();
        } catch (PanicError expected) {
        }

        assertSurplus(0, orec);
        assertUpdateBiased(orec);
        assertHasNoCommitLock(orec);
        assertReadonlyCount(0, orec);
        assertHasNoUpdateLock(orec);
    }

    @Test
    public void whenUpdateBiasedAndSurplusAndNotLocked() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);

        orec.___departAfterFailure();

        assertSurplus(0, orec);
        assertUpdateBiased(orec);
        assertHasNoCommitLock(orec);
        assertReadonlyCount(0, orec);
        assertHasNoUpdateLock(orec);
    }

    @Test
    public void whenUpdateBiasedAndSurplusAndLocked() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);
        orec.___arrive(1);
        orec.___tryLockAfterNormalArrive(1,true);

        orec.___departAfterFailure();

        assertSurplus(1, orec);
        assertUpdateBiased(orec);
        assertHasUpdateLock(orec);
        assertHasCommitLock(orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenReadBiasedAndLocked_thenPanicError() {
        FastOrec orec = makeReadBiased(new FastOrec());

        orec.___arrive(1);
        orec.___tryLockAfterNormalArrive(1,true);

        try {
            orec.___departAfterFailure();
            fail();
        } catch (PanicError expected) {
        }

        assertHasCommitLock(orec);
        assertSurplus(1, orec);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
        assertHasUpdateLock(orec);
    }

    @Test
    public void whenReadBiasedAndNotLocked_thenPanicError() {
        FastOrec orec = makeReadBiased(new FastOrec());

        try {
            orec.___departAfterFailure();
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
