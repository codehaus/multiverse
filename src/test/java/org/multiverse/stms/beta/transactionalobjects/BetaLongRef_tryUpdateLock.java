package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class BetaLongRef_tryUpdateLock {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenFree() {
        BetaRef orec = new BetaRef(stm);
        orec.___arrive(1);

        boolean result = orec.___tryLockAfterNormalArrive(1,true);
        assertTrue(result);
        assertHasCommitLock(orec);
        assertSurplus(1, orec);
        assertUpdateBiased(orec);
    }

    @Test
    public void whenFreeAndSurplus() {
        BetaRef orec = new BetaRef(stm);
        orec.___arrive(1);
        orec.___arrive(1);

        boolean result = orec.___tryLockAfterNormalArrive(1,true);
        assertTrue(result);
        assertHasCommitLock(orec);
        assertSurplus(2, orec);
        assertUpdateBiased(orec);
    }

    @Test
    public void whenLocked() {
        BetaRef orec = new BetaRef(stm);
        orec.___arrive(1);
        orec.___tryLockAfterNormalArrive(1,true);

        boolean result = orec.___tryLockAfterNormalArrive(1,true);
        assertFalse(result);
        assertHasCommitLock(orec);
        assertEquals(1, orec.___getSurplus());
        assertFalse(orec.___isReadBiased());
    }

    @Test
    public void whenReadBiasedMode() {
        BetaRef orec = makeReadBiased(new BetaRef(stm));

        orec.___arrive(1);
        boolean result = orec.___tryLockAfterNormalArrive(1,true);

        assertTrue(result);
        assertReadBiased(orec);
        assertHasCommitLock(orec);
        assertSurplus(1, orec);
    }

}
