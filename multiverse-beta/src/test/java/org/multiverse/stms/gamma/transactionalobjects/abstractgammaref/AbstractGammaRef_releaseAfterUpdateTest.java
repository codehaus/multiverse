package org.multiverse.stms.gamma.transactionalobjects.abstractgammaref;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaRefTranlocal;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.stms.gamma.GammaTestUtils.assertRefHasNoLocks;

public class AbstractGammaRef_releaseAfterUpdateTest implements GammaConstants {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    @Test
    public void whenNormalRef(){
        String initialValue = "initialValue";
        GammaRef<String> ref = new GammaRef<String>(stm, initialValue);

        GammaTransaction tx = stm.newDefaultTransaction();
        GammaRefTranlocal tranlocal = ref.openForWrite(tx, LOCKMODE_EXCLUSIVE);
        tranlocal.isDirty = true;

        ref.releaseAfterUpdate(tranlocal, tx.pool);

        assertNull(tranlocal.owner);
        assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
        assertNull(tranlocal.ref_value);
        assertNull(tranlocal.ref_oldValue);
        assertFalse(tranlocal.hasDepartObligation());
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenLongRef() {
        GammaLongRef ref = new GammaLongRef(stm, 0);

        GammaTransaction tx = stm.newDefaultTransaction();
        GammaRefTranlocal tranlocal = ref.openForWrite(tx, LOCKMODE_EXCLUSIVE);
        tranlocal.isDirty = true;

        ref.releaseAfterUpdate(tranlocal, tx.pool);

        assertNull(tranlocal.owner);
        assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
        assertFalse(tranlocal.hasDepartObligation());
        assertRefHasNoLocks(ref);
    }
}
