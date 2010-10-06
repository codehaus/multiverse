package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;
import org.multiverse.stms.beta.transactionalobjects.Tranlocal;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Veentjer
 */
public class Tranlocal_calculateIsDirtyTest implements BetaStmConstants {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenConstructed() {
        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        Tranlocal tranlocal = ref.___newTranlocal();
        tranlocal.isDirty = true;
        tranlocal.isConstructing = true;

        assertTrue(tranlocal.calculateIsDirty());
        assertTrue(tranlocal.isDirty);
    }

    @Test
    @Ignore
    public void whenCommuting() {
        BetaLongRef ref = new BetaLongRef(stm, 0);
        Tranlocal tranlocal = ref.___newTranlocal();
        tranlocal.isCommuting = true;

        assertFalse(tranlocal.calculateIsDirty());
        assertFalse(tranlocal.isDirty);
    }

    @Test
    public void whenCommitted() {
        BetaLongRef ref = new BetaLongRef(stm, 0);
        Tranlocal tranlocal = ref.___newTranlocal();
        tranlocal.isCommitted = true;

        assertFalse(tranlocal.calculateIsDirty());
        assertFalse(tranlocal.isDirty);
    }

    @Test
    public void whenNotDirty() {
        BetaLongRef ref = new BetaLongRef(stm, 0);
        LongRefTranlocal tranlocal = ref.___newTranlocal();
        tranlocal.value = 10;
        tranlocal.oldValue = 10;

        assertFalse(tranlocal.calculateIsDirty());
        assertFalse(tranlocal.isDirty);
    }

    @Test
    public void whenDirty() {
        BetaLongRef ref = new BetaLongRef(stm, 0);
        LongRefTranlocal tranlocal = ref.___newTranlocal();
        tranlocal.value = 10;
        tranlocal.oldValue = 5;

        assertTrue(tranlocal.calculateIsDirty());
        assertTrue(tranlocal.isDirty);
    }
}
