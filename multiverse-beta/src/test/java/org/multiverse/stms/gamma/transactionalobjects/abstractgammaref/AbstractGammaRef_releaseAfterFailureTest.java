package org.multiverse.stms.gamma.transactionalobjects.abstractgammaref;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.LockMode;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaRefTranlocal;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public class AbstractGammaRef_releaseAfterFailureTest implements GammaConstants {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    //todo: testen met en zonder depart obligation

    @Test
    public void whenCommuting() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.newDefaultTransaction();
        LongFunction function = mock(LongFunction.class);
        ref.commute(tx, function);
        GammaRefTranlocal tranlocal = tx.getRefTranlocal(ref);

        ref.releaseAfterFailure(tranlocal, tx.pool);

        assertRefHasNoLocks(ref);
        assertNull(tranlocal.owner);
        assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
        assertNull(tranlocal.headCallable);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenRead() {
        whenRead(LockMode.None);
        whenRead(LockMode.Read);
        whenRead(LockMode.Write);
        whenRead(LockMode.Exclusive);
    }

    public void whenRead(LockMode lockMode){
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = stm.newDefaultTransaction();
        GammaRefTranlocal tranlocal = ref.openForRead(tx, lockMode.asInt());

        ref.releaseAfterFailure(tranlocal,tx.pool);

        assertNull(tranlocal.owner);
        assertFalse(tranlocal.hasDepartObligation);
        assertEquals(LOCKMODE_NONE, tranlocal.lockMode);
        assertSurplus(ref, 0);
        assertLockMode(ref, LockMode.None);
        assertWriteBiased(ref);
        assertReadonlyCount(ref, 0);
    }

    @Test
    public void whenWrite() {
        whenWrite(LockMode.None);
        whenWrite(LockMode.Read);
        whenWrite(LockMode.Write);
        whenWrite(LockMode.Exclusive);
    }

    public void whenWrite(LockMode lockMode){
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = stm.newDefaultTransaction();
        GammaRefTranlocal tranlocal = ref.openForWrite(tx, lockMode.asInt());
        tranlocal.isDirty=true;

        ref.releaseAfterFailure(tranlocal,tx.pool);

        assertNull(tranlocal.owner);
        assertFalse(tranlocal.hasDepartObligation);
        assertEquals(LOCKMODE_NONE, tranlocal.lockMode);
        assertSurplus(ref, 0);
        assertLockMode(ref, LockMode.None);
        assertWriteBiased(ref);
        assertReadonlyCount(ref, 0);
    }

    @Test
    public void whenConstructing() {
        GammaTransaction tx = stm.newDefaultTransaction();
        GammaLongRef ref = new GammaLongRef(tx,0);
        GammaRefTranlocal tranlocal = tx.locate(ref);

        ref.releaseAfterFailure(tranlocal, tx.pool);

        assertNull(tranlocal.owner);
        assertFalse(tranlocal.hasDepartObligation);
        assertEquals(LOCKMODE_NONE, tranlocal.lockMode);
        assertSurplus(ref, 1);
        assertLockMode(ref, LockMode.Exclusive);
        assertWriteBiased(ref);
        assertReadonlyCount(ref, 0);
    }
}
