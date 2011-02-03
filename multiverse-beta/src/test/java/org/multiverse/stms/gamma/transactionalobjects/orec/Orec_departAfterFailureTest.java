package org.multiverse.stms.gamma.transactionalobjects.orec;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.PanicError;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.AbstractGammaObject;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;

import static org.junit.Assert.fail;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public class Orec_departAfterFailureTest {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    @Test
    public void whenUpdateBiasedAndNoSurplusAndNotLocked_thenPanicError() {
        AbstractGammaObject orec = new GammaLongRef(stm);

        try {
            orec.departAfterFailure();
            fail();
        } catch (PanicError expected) {
        }

        assertSurplus(orec, 0);
        assertUpdateBiased(orec);
        assertLockMode(orec, LOCKMODE_NONE);
        assertReadonlyCount(orec, 0);
    }

    @Test
    public void whenUpdateBiasedAndSurplusAndNotLocked() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.arrive(1);

        orec.departAfterFailure();

        assertSurplus(orec, 0);
        assertUpdateBiased(orec);
        assertLockMode(orec, LOCKMODE_NONE);
        assertReadonlyCount(orec, 0);
    }

    @Test
    public void whenUpdateBiasedAndSurplusAndLockedForCommit() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.arrive(1);
        orec.arrive(1);
        orec.tryLockAfterNormalArrive(1, LOCKMODE_EXCLUSIVE);

        orec.departAfterFailure();

        assertSurplus(orec, 1);
        assertUpdateBiased(orec);
        assertLockMode(orec, LOCKMODE_EXCLUSIVE);
        assertReadonlyCount(orec, 0);
    }

    @Test
    public void whenUpdateBiasedAndSurplusAndLockedForUpdate() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.arrive(1);
        orec.arrive(1);
        orec.tryLockAfterNormalArrive(1, LOCKMODE_WRITE);

        orec.departAfterFailure();

        assertSurplus(orec, 1);
        assertUpdateBiased(orec);
        assertLockMode(orec, LOCKMODE_WRITE);
        assertReadonlyCount(orec, 0);
    }


    @Test
    public void whenReadBiasedAndLockedForCommit_thenPanicError() {
        AbstractGammaObject orec = makeReadBiased(new GammaLongRef(stm));

        orec.arriveAndLock(1, LOCKMODE_EXCLUSIVE);

        try {
            orec.departAfterFailure();
            fail();
        } catch (PanicError expected) {
        }

        assertLockMode(orec, LOCKMODE_EXCLUSIVE);
        assertSurplus(orec, 1);
        assertReadBiased(orec);
        assertReadonlyCount(orec, 0);
    }

    @Test
    public void whenReadBiasedAndNotLocked_thenPanicError() {
        AbstractGammaObject orec = makeReadBiased(new GammaLongRef(stm));

        try {
            orec.departAfterFailure();
            fail();
        } catch (PanicError expected) {
        }

        assertLockMode(orec, LOCKMODE_NONE);
        assertSurplus(orec, 0);
        assertReadBiased(orec);
        assertReadonlyCount(orec, 0);
    }
}
