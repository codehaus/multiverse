package org.multiverse.stms.beta.orec;

import org.junit.Test;

import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class FastOrec_unlockByReadBiased {

    @Test
    public void whenSurplusAndReadBiasedAndLocked(){
        FastOrec orec = OrecTestUtils.makeReadBiased(new FastOrec());
        orec.___tryLockAndArrive(1,false);

        orec.___unlockByReadBiased();

        assertHasNoCommitLock(orec);
        assertReadBiased(orec);
        assertHasNoUpdateLock(orec);
        assertSurplus(1, orec);
        assertHasNoCommitLock(orec);
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
