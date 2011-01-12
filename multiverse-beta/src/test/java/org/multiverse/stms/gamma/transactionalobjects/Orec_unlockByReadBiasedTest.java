package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.LockMode;
import org.multiverse.stms.gamma.GammaStm;

import static org.multiverse.stms.gamma.GammaTestUtils.*;

public class Orec_unlockByReadBiasedTest {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    @Test
    public void whenReadLockedAcquiredOnce() {
        AbstractGammaObject orec = makeReadBiased(new GammaLongRef(stm));
        orec.tryLockAndArrive(1, LOCKMODE_READ);

        orec.unlockByReadBiased();
        assertLockMode(orec, LockMode.None);
        assertSurplus(orec, 1);
    }

    @Test
    public void whenReadLockAcquiredMultipleTimes() {
        AbstractGammaObject orec = makeReadBiased(new GammaLongRef(stm));
        orec.tryLockAndArrive(1, LOCKMODE_READ);
        orec.tryLockAndArrive(1, LOCKMODE_READ);
        orec.tryLockAndArrive(1, LOCKMODE_READ);

        orec.unlockByReadBiased();
        assertLockMode(orec, LockMode.None);
        assertReadLockCount(orec, 2);
        assertSurplus(orec, 1);
    }

    @Test
    public void whenWriteLockAcquired() {
        AbstractGammaObject orec = makeReadBiased(new GammaLongRef(stm));
        orec.tryLockAndArrive(1, LOCKMODE_WRITE);

        orec.unlockByReadBiased();
        assertLockMode(orec, LockMode.None);
        assertSurplus(orec, 1);
    }

    @Test
    public void whenCommitLockAcquired() {
        AbstractGammaObject orec = makeReadBiased(new GammaLongRef(stm));
        orec.tryLockAndArrive(1, LOCKMODE_COMMIT);

        orec.unlockByReadBiased();

        assertLockMode(orec, LOCKMODE_NONE);
        assertReadBiased(orec);
        assertSurplus(orec, 1);
    }

    @Test
    @Ignore
    public void whenSurplusAndUpdateBiasedAndLocked_thenPanicError() {

    }

    @Test
    @Ignore
    public void whenNoSurplusAndReadBiasedAndUnlocked_thenPanicError() {

    }

    @Test
    @Ignore
    public void whenNoSurplusAndUpdateBiasedAndUnlocked_thenPanicError() {

    }
}
