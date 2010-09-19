package org.multiverse.stms.beta.orec;

import org.junit.Test;
import org.multiverse.stms.beta.BetaStmConstants;

import static junit.framework.Assert.assertEquals;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;


/**
 * @author Peter Veentjer
 */
public class FastOrec_arriveTest implements BetaStmConstants {

    @Test
    public void whenUpdateBiasedNotLockedAndNoSurplus_thenNormalArrive() {
        FastOrec orec = new FastOrec();
        int result = orec.___arrive(1);

        assertEquals(ARRIVE_NORMAL, result);
        assertSurplus(1, orec);
        assertReadonlyCount(0, orec);
        assertUpdateBiased(orec);
        assertHasNoUpdateLock(orec);
        assertHasNoCommitLock(orec);
    }

    @Test
    public void whenUpdateBiasedAndNotLockedAndSurplus_thenNormalArrive() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);
        orec.___arrive(1);

        int result = orec.___arrive(1);

        assertEquals(ARRIVE_NORMAL, result);
        assertUpdateBiased(orec);
        assertSurplus(3, orec);
        assertReadonlyCount(0, orec);
        assertHasNoCommitLock(orec);
        assertHasNoUpdateLock(orec);
    }

    @Test
    public void whenUpdateBiasedAndLockedForCommit_thenLockNotFree() {
        FastOrec orec = new FastOrec();
        orec.___tryLockAndArrive(1, true);

        int result = orec.___arrive(1);

        assertEquals(ARRIVE_LOCK_NOT_FREE, result);
        assertSurplus(1, orec);
        assertReadonlyCount(0, orec);
        assertUpdateBiased(orec);
        assertHasNoUpdateLock(orec);
        assertHasCommitLock(orec);
    }

    @Test
    public void whenUpdateBiasedAndLockedForUpdate_thenUnregisteredArrive() {
        FastOrec orec = new FastOrec();
        orec.___tryLockAndArrive(1, false);

        int result = orec.___arrive(1);

        assertEquals(ARRIVE_NORMAL, result);
        assertSurplus(2, orec);
        assertReadonlyCount(0, orec);
        assertUpdateBiased(orec);
        assertHasUpdateLock(orec);
        assertHasNoCommitLock(orec);
    }

    @Test
    public void whenReadBiasedAndLockedForCommit() {
        FastOrec orec = OrecTestUtils.makeReadBiased(new FastOrec());
        orec.___arrive(1);
        orec.___tryLockAfterNormalArrive(1, true);

        int result = orec.___arrive(1);

        assertEquals(ARRIVE_LOCK_NOT_FREE, result);
        assertSurplus(1, orec);
        assertReadonlyCount(0, orec);
        assertHasCommitLock(orec);
        assertHasNoUpdateLock(orec);
        assertReadBiased(orec);        
    }

    @Test
    public void whenReadBiasedAndNoSurplus() {
        FastOrec orec = OrecTestUtils.makeReadBiased(new FastOrec());

        int result = orec.___arrive(1);

        assertEquals(ARRIVE_UNREGISTERED, result);
        assertHasNoCommitLock(orec);
        assertSurplus(1, orec);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
        assertHasNoUpdateLock(orec);
    }

    @Test
    public void whenReadBiasedAndSurplus_thenCallIgnored() {
        FastOrec orec = OrecTestUtils.makeReadBiased(new FastOrec());
        orec.___arrive(1);

        int result = orec.___arrive(1);

        assertEquals(ARRIVE_UNREGISTERED, result);
        assertHasNoCommitLock(orec);
        assertSurplus(1, orec);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
        assertHasNoUpdateLock(orec);
    }
}
