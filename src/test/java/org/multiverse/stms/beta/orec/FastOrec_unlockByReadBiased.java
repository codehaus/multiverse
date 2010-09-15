package org.multiverse.stms.beta.orec;

import org.junit.Test;

import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class FastOrec_unlockByReadBiased {

    @Test
    public void whenSurplusAndReadBiasedAndLocked(){
        FastOrec orec = OrecTestUtils.makeReadBiased(new FastOrec());
        orec.___tryLockAndArrive(1);

        orec.___unlockByReadBiased();

        assertUnlocked(orec);
        assertReadBiased(orec);
        assertNotProtectedAgainstUpdate(orec);
        assertSurplus(1, orec);
        assertUnlocked(orec);
    }

    @Test
    public void whenSurplusAndUpdateBiasedAndLocked_thenPanicError(){

    }

    @Test
    public void whenNoSurplusAndReadBiasedAndUnlocked_thenPanicError(){

    }

    @Test
    public void whenNoSurplusAndUpdateBiasedAndUnlocked_thenPanicError(){

    }

}
