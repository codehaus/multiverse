package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.multiverse.stms.beta.BetaStmTestUtils.*;

public class VeryAbstractBetaTransactionalObject_atomicIsFreeTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenIsFree() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        boolean result = ref.atomicIsUnlocked();

        assertTrue(result);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenIsEnsured() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquireWriteLock(tx);

        boolean result = ref.atomicIsUnlocked();

        assertFalse(result);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasUpdateLock(ref, tx);
    }

    @Test
    public void whenIsPrivatized() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquireCommitLock(tx);

        boolean result = ref.atomicIsUnlocked();

        assertFalse(result);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasCommitLock(ref, tx);
    }
}
