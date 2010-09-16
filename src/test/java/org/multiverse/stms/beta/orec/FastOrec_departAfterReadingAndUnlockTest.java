package org.multiverse.stms.beta.orec;

import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.PanicError;

import static org.junit.Assert.fail;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class FastOrec_departAfterReadingAndUnlockTest {

    @Test
    public void whenMultipleArrivesAndLocked(){
        FastOrec orec = new FastOrec();
        orec.___arrive(1);
        orec.___arrive(2);
        orec.___tryLockAndArrive(1,false);

        orec.___departAfterReadingAndUnlock();
        assertSurplus(2, orec);
        assertHasNoCommitLock(orec);
        assertHasNoUpdateLock(orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(1, orec);
    }

    @Test
    public void whenSuccess() {
        FastOrec orec = new FastOrec();
        orec.___tryLockAndArrive(1,false);

        orec.___departAfterReadingAndUnlock();
        assertSurplus(0, orec);
        assertHasNoCommitLock(orec);
        assertHasNoUpdateLock(orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(1, orec);
    }

    @Test
    @Ignore
    public void whenLockedAndReadBiased(){

    }

    @Test
    public void whenNotLockedAndNoSurplus_thenPanicError() {
        FastOrec orec = new FastOrec();

        try {
            orec.___departAfterReadingAndUnlock();
            fail();
        } catch (PanicError expected) {
        }

        assertSurplus(0, orec);
        assertHasNoCommitLock(orec);
        assertHasNoUpdateLock(orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenNotLockedAndSurplus_thenPanicError() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);

        try {
            orec.___departAfterReadingAndUnlock();
            fail();
        } catch (PanicError expected) {
        }

        assertSurplus(1, orec);
        assertHasNoCommitLock(orec);
        assertHasNoUpdateLock(orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
    }
}
