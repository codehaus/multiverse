package org.multiverse.stms.beta.refs;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.conflictcounters.GlobalConflictCounter;
import org.multiverse.stms.beta.orec.OrecTestUtils;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Peter Veentjer
 */
public class LongRef_Orec_departAfterUpdateAndReleaseLock {

    private GlobalConflictCounter globalConflictCounter;

    @Before
    public void setUp() {
        globalConflictCounter = new GlobalConflictCounter(8);
    }

    @Test
    public void whenNotLockedAndNoSurplus_thenIllegalStateException() {
        Ref orec = new Ref();

        long oldConflictCount = globalConflictCounter.count();

        try {
            orec.departAfterUpdateAndReleaseLock(globalConflictCounter, null);
            fail();
        } catch (IllegalStateException expected) {
        }

        assertEquals(oldConflictCount, globalConflictCounter.count());
        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertSurplus(0, orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
        OrecTestUtils.assertUpdateBiased(orec);
    }

    @Test
    public void whenNotLockedAndSurplus_thenIllegalStateException() {
        Ref orec = new Ref();
        orec.arrive(1);
        orec.arrive(1);

        long oldConflictCount = globalConflictCounter.count();
        try {
            orec.departAfterUpdateAndReleaseLock(globalConflictCounter, null);
            fail();
        } catch (IllegalStateException expected) {
        }

        assertEquals(oldConflictCount, globalConflictCounter.count());
        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertSurplus(2, orec);
        OrecTestUtils.assertUpdateBiased(orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
    }

    @Test
    public void whenLockedAndNoAdditionalSurplus() {
        Ref orec = new Ref();
        orec.arrive(1);
        orec.tryUpdateLock(1);

        long oldConflictCount = globalConflictCounter.count();

        long result = orec.departAfterUpdateAndReleaseLock(globalConflictCounter, null);

        assertEquals(0, result);
        assertEquals(oldConflictCount, globalConflictCounter.count());
        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertSurplus(0, orec);
        OrecTestUtils.assertUpdateBiased(orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
    }

    @Test
    public void whenLockedAndAdditionalSurplus() {
        Ref orec = new Ref();
        orec.arrive(1);
        orec.arrive(1);
        orec.arrive(1);
        orec.tryUpdateLock(1);

        long oldConflictCount = globalConflictCounter.count();

        long result = orec.departAfterUpdateAndReleaseLock(globalConflictCounter, null);

        assertEquals(oldConflictCount + 1, globalConflictCounter.count());
        assertEquals(2, result);
        OrecTestUtils.assertUnlocked(orec);
        OrecTestUtils.assertSurplus(2, orec);
        OrecTestUtils.assertUpdateBiased(orec);
        OrecTestUtils.assertReadonlyCount(0, orec);
    }
}
