package org.multiverse.stms.beta.orec;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.PanicError;
import org.multiverse.stms.beta.conflictcounters.GlobalConflictCounter;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertSurplus;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertUnlocked;

/**
 * @author Peter Veentjer
 */
public class UnsafeOrec_departAfterUpdateAndReleaseLockTest {
    private GlobalConflictCounter globalConflictCounter;

    @Before
    public void setUp() {
        globalConflictCounter = new GlobalConflictCounter(8);
    }

    @Test
    public void whenNotLockedAndNoSurplus_thenPanicError() {
        UnsafeOrec orec = new UnsafeOrec();

        long oldConflictCount = globalConflictCounter.count();

        try {
            orec.___departAfterUpdateAndReleaseLock(globalConflictCounter, null);
            fail();
        } catch (PanicError expected) {
        }

        assertEquals(oldConflictCount, globalConflictCounter.count());
        assertUnlocked(orec);
        assertSurplus(0, orec);
    }

    @Test
    public void whenNotLockedAndSurplus_thenPanicError() {
        UnsafeOrec orec = new UnsafeOrec();
        orec.___arrive(1);
        orec.___arrive(1);

        long oldConflictCount = globalConflictCounter.count();
        try {
            orec.___departAfterUpdateAndReleaseLock(globalConflictCounter, null);
            fail();
        } catch (PanicError expected) {
        }

        assertEquals(oldConflictCount, globalConflictCounter.count());
        assertUnlocked(orec);
        assertSurplus(2, orec);
    }

    @Test
    public void whenLockedAndNoAdditionalSurplus() {
        UnsafeOrec orec = new UnsafeOrec();
        orec.___arrive(1);
        orec.___tryLockAfterArrive(1);

        long oldConflictCount = globalConflictCounter.count();

        long result = orec.___departAfterUpdateAndReleaseLock(globalConflictCounter, null);

        assertEquals(0, result);
        assertEquals(oldConflictCount, globalConflictCounter.count());
        assertUnlocked(orec);
        assertSurplus(0, orec);
    }

    @Test
    public void whenLockedAndAdditionalSurplus() {
        UnsafeOrec orec = new UnsafeOrec();
        orec.___arrive(1);
        orec.___arrive(1);
        orec.___arrive(1);
        orec.___tryLockAfterArrive(1);

        long oldConflictCount = globalConflictCounter.count();

        long result = orec.___departAfterUpdateAndReleaseLock(globalConflictCounter, null);

        assertEquals(oldConflictCount + 1, globalConflictCounter.count());
        assertEquals(2, result);
        assertUnlocked(orec);
        assertSurplus(2, orec);
    }
}
