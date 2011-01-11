package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.PanicError;
import org.multiverse.stms.gamma.GammaStm;

import static org.junit.Assert.fail;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public class Orec_departAfterReadingTest {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    @Test
    public void whenNoSurplus_thenPanicError() {
        AbstractGammaObject orec = new GammaLongRef(stm);

        try {
            orec.departAfterReading();
            fail();
        } catch (PanicError expected) {
        }

        assertSurplus(0, orec);
        assertReadonlyCount(0, orec);
        assertUpdateBiased(orec);
        assertLockMode(orec, LOCKMODE_NONE);
    }

    @Test
    public void whenMuchSurplus() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.arrive(1);
        orec.arrive(1);

        orec.departAfterReading();

        assertSurplus(1, orec);
        assertReadonlyCount(1, orec);
        assertUpdateBiased(orec);
        assertLockMode(orec, LOCKMODE_NONE);

    }

    @Test
    public void whenLockedForRead() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.arrive(1);
        orec.arrive(1);
        orec.tryLockAfterNormalArrive(1, LOCKMODE_READ);

        orec.departAfterReading();

        assertSurplus(1, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(1, orec);
        assertLockMode(orec, LOCKMODE_READ);
    }

    @Test
    public void whenLockedForCommit() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.arrive(1);
        orec.arrive(1);
        orec.tryLockAfterNormalArrive(1, LOCKMODE_COMMIT);

        orec.departAfterReading();

        assertSurplus(1, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(1, orec);
        assertLockMode(orec, LOCKMODE_COMMIT);
    }

    @Test
    public void whenLockedForUpdate() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.arrive(1);
        orec.arrive(1);
        orec.tryLockAfterNormalArrive(1, LOCKMODE_WRITE);

        orec.departAfterReading();

        assertSurplus(1, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(1, orec);
        assertLockMode(orec, LOCKMODE_WRITE);
    }

    @Test
    public void whenReadBiasedAndLockedForCommit_thenPanicError() {
        AbstractGammaObject orec = makeReadBiased(new GammaLongRef(stm));
        orec.tryLockAndArrive(1, LOCKMODE_COMMIT);

        try {
            orec.departAfterReading();
            fail();
        } catch (PanicError expected) {

        }

        assertLockMode(orec, LOCKMODE_COMMIT);
        assertSurplus(1, orec);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenReadBiasedAndUnlocked_thenPanicError() {
        AbstractGammaObject orec = makeReadBiased(new GammaLongRef(stm));

        try {
            orec.departAfterReading();
            fail();
        } catch (PanicError expected) {

        }

        assertLockMode(orec, LOCKMODE_NONE);
        assertSurplus(0, orec);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
    }
}
