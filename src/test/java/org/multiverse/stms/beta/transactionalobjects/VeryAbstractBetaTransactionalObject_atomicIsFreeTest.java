package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.stms.beta.BetaStmTestUtils.assertVersionAndValue;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class VeryAbstractBetaTransactionalObject_atomicIsFreeTest {
    private BetaStm stm;

    @Before
    public void setUp(){
        stm = new BetaStm();
    }

    @Test
    public void whenIsFree(){
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        boolean result = ref.atomicIsFree();

        assertTrue(result);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertNull(ref.___getLockOwner());
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
    }

    @Test
    public void whenIsEnsured(){
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.ensure(tx);

        boolean result = ref.atomicIsFree();

        assertFalse(result);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSame(tx, ref.___getLockOwner());
        assertHasNoCommitLock(ref);
        assertHasUpdateLock(ref);
    }

    @Test
    public void whenIsPrivatized(){
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.privatize(tx);

        boolean result = ref.atomicIsFree();

        assertFalse(result);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSame(tx, ref.___getLockOwner());
        assertHasCommitLock(ref);
        assertHasNoUpdateLock(ref);
    }
}
