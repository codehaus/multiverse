package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;

import static junit.framework.Assert.assertEquals;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public class Orec_arriveTest implements GammaConstants {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    @Test
    public void whenUpdateBiasedNotLockedAndNoSurplus_thenNormalArrive() {
        AbstractGammaObject orec = new GammaLongRef(stm, 0);
        int result = orec.arrive(1);

        assertEquals(ARRIVE_NORMAL, result);
        assertSurplus(orec, 1);
        assertReadonlyCount(0, orec);
        assertUpdateBiased(orec);
        assertLockMode(orec, LOCKMODE_NONE);
    }

    @Test
    public void whenUpdateBiasedAndNotLockedAndSurplus_thenNormalArrive() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.arrive(1);
        orec.arrive(1);

        int result = orec.arrive(1);

        assertEquals(ARRIVE_NORMAL, result);
        assertUpdateBiased(orec);
        assertSurplus(orec, 3);
        assertReadonlyCount(0, orec);
        assertLockMode(orec, LOCKMODE_NONE);
    }

    @Test
    public void whenUpdateBiasedAndLockedForCommit_thenLockNotFree() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.tryLockAndArrive(1, LOCKMODE_COMMIT);

        int result = orec.arrive(1);

        assertEquals(ARRIVE_LOCK_NOT_FREE, result);
        assertSurplus(orec, 1);
        assertReadonlyCount(0, orec);
        assertUpdateBiased(orec);
        assertLockMode(orec, LOCKMODE_COMMIT);
    }

    @Test
    public void whenUpdateBiasedAndLockedForUpdate_thenUnregisteredArrive() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.tryLockAndArrive(1, LOCKMODE_WRITE);

        int result = orec.arrive(1);

        assertEquals(ARRIVE_NORMAL, result);
        assertSurplus(orec, 2);
        assertReadonlyCount(0, orec);
        assertUpdateBiased(orec);
        assertLockMode(orec, LOCKMODE_WRITE);
    }

    @Test
    public void whenReadBiasedAndNoSurplus() {
        AbstractGammaObject orec = makeReadBiased(new GammaLongRef(stm));

        int result = orec.arrive(1);

        assertEquals(ARRIVE_UNREGISTERED, result);
        assertLockMode(orec, LOCKMODE_NONE);
        assertSurplus(orec, 1);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenReadBiasedAndSurplus_thenCallIgnored() {
        AbstractGammaObject orec = makeReadBiased(new GammaLongRef(stm));
        orec.arrive(1);

        int result = orec.arrive(1);

        assertEquals(ARRIVE_UNREGISTERED, result);
        assertLockMode(orec, LOCKMODE_NONE);
        assertSurplus(orec, 1);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
    }
}
