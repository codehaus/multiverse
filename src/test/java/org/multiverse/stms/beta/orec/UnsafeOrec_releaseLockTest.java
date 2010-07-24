package org.multiverse.stms.beta.orec;

import org.junit.Test;

import static org.junit.Assert.fail;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class UnsafeOrec_releaseLockTest {

    @Test
    public void whenNoSurplusAndNotLocked_thenIllegalStateException() {
        UnsafeOrec orec = new UnsafeOrec();

        try {
            orec.unlockAfterBecomingReadBiased();
            fail();
        } catch (IllegalStateException expected) {
        }

        assertUnlocked(orec);
        assertUpdateBiased(orec);
        assertSurplus(0, orec);
    }

    @Test
    public void whenSurplusAndNotLocked_thenIllegalStateException() {
        UnsafeOrec orec = new UnsafeOrec();
        orec.arrive(1);


        try {
            orec.unlockAfterBecomingReadBiased();
            fail();
        } catch (IllegalStateException expected) {
        }

        assertUnlocked(orec);
        assertUpdateBiased(orec);
        assertSurplus(1, orec);
    }

    @Test
    public void whenLocked() {
        UnsafeOrec orec = new UnsafeOrec();
        orec.arrive(1);
        orec.tryUpdateLock(1);

        orec.unlockAfterBecomingReadBiased();

        assertUnlocked(orec);
        assertUpdateBiased(orec);
        assertSurplus(1, orec);
    }
}
