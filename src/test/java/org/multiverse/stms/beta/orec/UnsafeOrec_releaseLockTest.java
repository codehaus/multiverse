package org.multiverse.stms.beta.orec;

import org.junit.Test;
import org.multiverse.api.exceptions.PanicError;

import static org.junit.Assert.fail;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class UnsafeOrec_releaseLockTest {

    @Test
    public void whenNoSurplusAndNotLocked_thenPanicError() {
        UnsafeOrec orec = new UnsafeOrec();

        try {
            orec.___unlockAfterBecomingReadBiased();
            fail();
        } catch (PanicError expected) {
        }

        assertUnlocked(orec);
        assertUpdateBiased(orec);
        assertSurplus(0, orec);
    }

    @Test
    public void whenSurplusAndNotLocked_thenPanicError() {
        UnsafeOrec orec = new UnsafeOrec();
        orec.___arrive(1);


        try {
            orec.___unlockAfterBecomingReadBiased();
            fail();
        } catch (PanicError expected) {
        }

        assertUnlocked(orec);
        assertUpdateBiased(orec);
        assertSurplus(1, orec);
    }

    @Test
    public void whenLocked() {
        UnsafeOrec orec = new UnsafeOrec();
        orec.___arrive(1);
        orec.___tryUpdateLock(1);

        orec.___unlockAfterBecomingReadBiased();

        assertUnlocked(orec);
        assertUpdateBiased(orec);
        assertSurplus(1, orec);
    }
}
