package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.PanicError;
import org.multiverse.stms.gamma.GammaStm;

import static org.junit.Assert.fail;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public class Orec_departAfterReadingAndUnlockTest {
    
      private GammaStm stm;

    @Before
    public void setUp(){
        stm = new GammaStm();
    }

    @Test
    public void whenMultipleArrivesAndLockedForCommit() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.arrive(1);
        orec.arrive(2);
        orec.tryLockAndArrive(1, LOCKMODE_COMMIT);

        orec.departAfterReadingAndUnlock();

        assertSurplus(2, orec);
        assertLockMode(orec, LOCKMODE_NONE);
        assertUpdateBiased(orec);
        assertReadonlyCount(1, orec);
    }

    @Test
    public void whenMultipleArrivesAndLockedForUpdate() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.arrive(1);
        orec.arrive(2);
        orec.tryLockAndArrive(1, LOCKMODE_WRITE);

        orec.departAfterReadingAndUnlock();

        assertSurplus(2, orec);
        assertLockMode(orec, LOCKMODE_NONE);
        assertUpdateBiased(orec);
        assertReadonlyCount(1, orec);
    }

    @Test
    public void whenSuccess() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.tryLockAndArrive(1, LOCKMODE_COMMIT);

        orec.departAfterReadingAndUnlock();
        assertSurplus(0, orec);
        assertLockMode(orec, LOCKMODE_NONE);
        assertUpdateBiased(orec);
        assertReadonlyCount(1, orec);
    }

    @Test
    public void whenLockedAndReadBiased() {
        AbstractGammaObject orec = makeReadBiased(new GammaLongRef(stm));
        orec.tryLockAndArrive(1, LOCKMODE_COMMIT);

        try {
            orec.departAfterReadingAndUnlock();
            fail();
        } catch (PanicError expected) {
        }

        assertSurplus(1, orec);
        assertLockMode(orec, LOCKMODE_COMMIT);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenNotLockedAndNoSurplus_thenPanicError() {
        AbstractGammaObject orec = new GammaLongRef(stm);

        try {
            orec.departAfterReadingAndUnlock();
            fail();
        } catch (PanicError expected) {
        }

        assertSurplus(0, orec);
        assertLockMode(orec, LOCKMODE_NONE);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenNotLockedAndSurplus_thenPanicError() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.arrive(1);

        try {
            orec.departAfterReadingAndUnlock();
            fail();
        } catch (PanicError expected) {
        }

        assertSurplus(1, orec);
        assertLockMode(orec, LOCKMODE_NONE);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
    }
}
