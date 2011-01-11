package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.gamma.GammaStm;

import static org.multiverse.stms.gamma.GammaTestUtils.*;

public class Orec_unlockByReadBiasedTest {
    
     private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    @Test
    public void whenSurplusAndReadBiasedAndLocked() {
        AbstractGammaObject orec = makeReadBiased(new GammaLongRef(stm));
        orec.tryLockAndArrive(1, LOCKMODE_COMMIT);

        orec.unlockByReadBiased();

        assertLockMode(orec, LOCKMODE_NONE);
        assertReadBiased(orec);
        assertSurplus(1, orec);
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
