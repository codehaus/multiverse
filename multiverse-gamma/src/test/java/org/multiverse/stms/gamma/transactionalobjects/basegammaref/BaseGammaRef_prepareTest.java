package org.multiverse.stms.gamma.transactionalobjects.basegammaref;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.LockMode;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaRefTranlocal;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.*;

public class BaseGammaRef_prepareTest implements GammaConstants {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    @Test
    @Ignore
    public void whenNormalReadButLockedByOther() {

    }

    @Test
    public void whenNormalRead() {
        whenNormalRead(LockMode.None);
        whenNormalRead(LockMode.Read);
        whenNormalRead(LockMode.Write);
        whenNormalRead(LockMode.Exclusive);
    }

    public void whenNormalRead(LockMode lockMode) {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = stm.newTransactionFactoryBuilder()
                .setFat()
                .newTransactionFactory()
                .newTransaction();
        GammaRefTranlocal tranlocal = ref.openForRead(tx, lockMode.asInt());

        boolean success = ref.prepare(tx, tranlocal);

        assertTrue(success);
        assertSame(ref, tranlocal.owner);
        assertEquals(lockMode.asInt(), tranlocal.getLockMode());
        assertEquals(TRANLOCAL_READ, tranlocal.mode);
        assertFalse(tranlocal.isDirty);
        assertFalse(tranlocal.writeSkewCheck);
        assertNull(tranlocal.headCallable);
    }

    @Test
    public void whenNonDirtyWriteAndDirtyCheckEnabled() {
        whenNonDirtyWriteAndDirtyCheckEnabled(LockMode.None);
        whenNonDirtyWriteAndDirtyCheckEnabled(LockMode.Read);
        whenNonDirtyWriteAndDirtyCheckEnabled(LockMode.Write);
        whenNonDirtyWriteAndDirtyCheckEnabled(LockMode.Exclusive);
    }

    public void whenNonDirtyWriteAndDirtyCheckEnabled(LockMode lockMode) {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = stm.newTransactionFactoryBuilder()
                .setFat()
                .setDirtyCheckEnabled(true)
                .newTransactionFactory()
                .newTransaction();

        GammaRefTranlocal tranlocal = ref.openForWrite(tx, lockMode.asInt());

        boolean success = ref.prepare(tx, tranlocal);

        assertTrue(success);
        assertSame(ref, tranlocal.owner);
        assertEquals(lockMode.asInt(), tranlocal.getLockMode());
        assertEquals(TRANLOCAL_WRITE, tranlocal.mode);
        assertFalse(tranlocal.isDirty);
        assertFalse(tranlocal.writeSkewCheck);
        assertNull(tranlocal.headCallable);
    }

    @Test
    public void whenNormalDirtyWriteAndDirtyCheckEnabled() {
        whenNormalDirtyWrite(LockMode.None, true);
        whenNormalDirtyWrite(LockMode.Read, true);
        whenNormalDirtyWrite(LockMode.Write, true);
        whenNormalDirtyWrite(LockMode.Exclusive, true);
    }

    @Test
    public void whenNormalDirtyWriteAndDirtyCheckDisabled() {
        whenNormalDirtyWrite(LockMode.None, false);
        whenNormalDirtyWrite(LockMode.Read, false);
        whenNormalDirtyWrite(LockMode.Write, false);
        whenNormalDirtyWrite(LockMode.Exclusive, false);
    }

    public void whenNormalDirtyWrite(LockMode lockMode, boolean dirtyCheck) {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = stm.newTransactionFactoryBuilder()
                .setFat()
                .setDirtyCheckEnabled(dirtyCheck)
                .newTransactionFactory()
                .newTransaction();

        GammaRefTranlocal tranlocal = ref.openForWrite(tx, lockMode.asInt());
        tranlocal.long_value++;

        boolean success = ref.prepare(tx, tranlocal);

        assertTrue(success);
        assertSame(ref, tranlocal.owner);
        assertEquals(LOCKMODE_EXCLUSIVE, tranlocal.getLockMode());
        assertEquals(TRANLOCAL_WRITE, tranlocal.mode);
        assertTrue(tranlocal.isDirty);
        assertFalse(tranlocal.writeSkewCheck);
        assertNull(tranlocal.headCallable);
    }

    @Test
    public void whenNonDirtyWriteAndDirtyCheckDisabled() {
        whenNonDirtyWrite(LockMode.None);
        whenNonDirtyWrite(LockMode.Read);
        whenNonDirtyWrite(LockMode.Write);
        whenNonDirtyWrite(LockMode.Exclusive);
    }

    public void whenNonDirtyWrite(LockMode lockMode) {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = stm.newTransactionFactoryBuilder()
                .setFat()
                .setDirtyCheckEnabled(false)
                .newTransactionFactory()
                .newTransaction();

        GammaRefTranlocal tranlocal = ref.openForWrite(tx, lockMode.asInt());

        boolean success = ref.prepare(tx, tranlocal);

        assertTrue(success);
        assertSame(ref, tranlocal.owner);
        assertEquals(LOCKMODE_EXCLUSIVE, tranlocal.getLockMode());
        assertEquals(TRANLOCAL_WRITE, tranlocal.mode);
        assertTrue(tranlocal.isDirty);
        assertFalse(tranlocal.writeSkewCheck);
        assertNull(tranlocal.headCallable);
    }

    @Test
    @Ignore
    public void whenConstructed() {

    }

    @Test
    @Ignore
    public void whenCommuting() {

    }
}
