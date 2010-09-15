package org.multiverse.stms.beta.orec;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.PanicError;
import org.multiverse.stms.beta.conflictcounters.GlobalConflictCounter;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class FastOrec_departAfterUpdateAndUnlock {

    private GlobalConflictCounter globalConflictCounter;

    @Before
    public void setUp() {
        globalConflictCounter = new GlobalConflictCounter(8);
    }

    @Test
    public void whenNotLockedAndNoSurplus_thenPanicError() {
        FastOrec orec = new FastOrec();

        long oldConflictCount = globalConflictCounter.count();

        try {
            orec.___departAfterUpdateAndUnlock(globalConflictCounter, null);
            fail();
        } catch (PanicError expected) {
        }

        assertEquals(oldConflictCount, globalConflictCounter.count());
        assertUnlocked(orec);
        assertSurplus(0, orec);
        assertReadonlyCount(0, orec);
        assertUpdateBiased(orec);
        assertNotProtectedAgainstUpdate(orec);
    }

    @Test
    public void whenNotLockedAndSurplus_thenPanicError() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);
        orec.___arrive(1);

        long oldConflictCount = globalConflictCounter.count();
        try {
            orec.___departAfterUpdateAndUnlock(globalConflictCounter, null);
            fail();
        } catch (PanicError expected) {
        }

        assertEquals(oldConflictCount, globalConflictCounter.count());
        assertUnlocked(orec);
        assertSurplus(2, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertNotProtectedAgainstUpdate(orec);
    }

    @Test
    public void whenLockedAndNoAdditionalSurplus() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);
        orec.___tryLockAfterNormalArrive(1);

        long oldConflictCount = globalConflictCounter.count();

        long result = orec.___departAfterUpdateAndUnlock(globalConflictCounter, null);

        assertEquals(0, result);
        assertEquals(oldConflictCount, globalConflictCounter.count());
        assertUnlocked(orec);
        assertSurplus(0, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertNotProtectedAgainstUpdate(orec);
    }

    @Test
    public void whenLockedAndAdditionalSurplus() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);
        orec.___arrive(1);
        orec.___arrive(1);
        orec.___tryLockAfterNormalArrive(1);

        long oldConflictCount = globalConflictCounter.count();

        long result = orec.___departAfterUpdateAndUnlock(globalConflictCounter, null);

        assertEquals(oldConflictCount + 1, globalConflictCounter.count());
        assertEquals(2, result);
        assertUnlocked(orec);
        assertSurplus(2, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertNotProtectedAgainstUpdate(orec);
    }
}
