package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.stms.gamma.GammaTestUtils.assertRefHasNoLocks;

public class AbstractGammaObject_releaseAfterUpdateTest implements GammaConstants {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    @Test
    public void test() {
        GammaLongRef ref = new GammaLongRef(stm, 0);

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaRefTranlocal tranlocal = ref.openForWrite(tx, LOCKMODE_COMMIT);
        tranlocal.isDirty = true;

        ref.releaseAfterUpdate(tranlocal, tx.pool);

        assertNull(tranlocal.owner);
        assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
        assertFalse(tranlocal.hasDepartObligation());
        assertRefHasNoLocks(ref);
    }
}
