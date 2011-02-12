package org.multiverse.stms.gamma.transactionalobjects.abstractgammaref;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaRefTranlocal;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.multiverse.stms.gamma.GammaTestUtils.assertRefHasExclusiveLock;
import static org.multiverse.stms.gamma.GammaTestUtils.assertVersionAndValue;

public class AbstractGammaRef_prepareTest implements GammaConstants {

    private GammaStm stm;

    @Before
    public void setUp(){
        stm = new GammaStm();
    }

    @Test
    public void whenLockedAndDirty(){
        whenLockAndDirty(true, LOCKMODE_NONE);
        whenLockAndDirty(true, LOCKMODE_READ);
        whenLockAndDirty(true, LOCKMODE_WRITE);
        whenLockAndDirty(true, LOCKMODE_EXCLUSIVE);

        whenLockAndDirty(false, LOCKMODE_NONE);
        whenLockAndDirty(false, LOCKMODE_READ);
        whenLockAndDirty(false, LOCKMODE_WRITE);
        whenLockAndDirty(false, LOCKMODE_EXCLUSIVE);
    }

    public void whenLockAndDirty(boolean dirtyCheck, int lockMode){
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.newTransactionFactoryBuilder()
                .setDirtyCheckEnabled(dirtyCheck)
                .setSpeculative(false)
                .newTransactionFactory()
                .newTransaction();

        GammaRefTranlocal tranlocal = ref.openForWrite(tx, lockMode);
        tranlocal.long_value++;
        boolean result = ref.prepare(tx, tranlocal);

        assertTrue(result);
        assertTrue(tranlocal.isDirty);
        assertEquals(LOCKMODE_EXCLUSIVE,tranlocal.lockMode);
        assertRefHasExclusiveLock(ref, tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }
}
