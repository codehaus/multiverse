package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.stms.beta.BetaStmTestUtils.assertVersionAndValue;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertHasNoCommitLock;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertHasNoUpdateLock;

public class LongTranlocal_openForWriteTest implements BetaStmConstants {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenReadonlyTransaction_thenReadonlyException() {
        long initialValue = 10;
        BetaLongRef ref = new BetaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.createTransactionFactoryBuilder()
                .setReadonly(true)
                .setSpeculativeConfigurationEnabled(false)
                .build()
                .newTransaction();

        LongRefTranlocal tranlocal = tx.open(ref);

        try {
            tranlocal.openForWrite(LOCKMODE_NONE);
            fail();
        } catch (ReadonlyException expected) {

        }

        assertNull(ref.___getLockOwner());
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenReadonlyTransactionAndAlreadyOpenedForRead_thenReadonlyException() {
        long initialValue = 10;
        BetaLongRef ref = new BetaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.createTransactionFactoryBuilder()
                .setReadonly(true)
                .setSpeculativeConfigurationEnabled(false)
                .build()
                .newTransaction();

        LongRefTranlocal tranlocal = tx.open(ref);
        tranlocal.openForRead(LOCKMODE_NONE);
        try {
            tranlocal.openForWrite(LOCKMODE_NONE);
            fail();
        } catch (ReadonlyException expected) {

        }

        assertNull(ref.___getLockOwner());
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenFirstTime() {
        long initialValue = 10;
        BetaLongRef ref = new BetaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        LongRefTranlocal tranlocal = tx.open(ref);

        tranlocal.openForWrite(LOCKMODE_NONE);

        assertFalse(tranlocal.isReadonly());
        assertEquals(initialVersion, tranlocal.version);
        assertEquals(initialValue, tranlocal.value);
        assertEquals(initialValue, tranlocal.oldValue);
        assertNull(ref.___getLockOwner());
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenAlreadyOpenedForRead() {
        long initialValue = 10;
        BetaLongRef ref = new BetaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        LongRefTranlocal tranlocal = tx.open(ref);

        tranlocal.openForRead(LOCKMODE_NONE);
        tranlocal.openForWrite(LOCKMODE_NONE);

        assertFalse(tranlocal.isReadonly());
        assertEquals(initialVersion, tranlocal.version);
        assertEquals(initialValue, tranlocal.value);
        assertEquals(initialValue, tranlocal.oldValue);
        assertNull(ref.___getLockOwner());
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenAlreadyOpenedForWrite() {
        long initialValue = 10;
        BetaLongRef ref = new BetaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        LongRefTranlocal tranlocal = tx.open(ref);

        tranlocal.openForWrite(LOCKMODE_NONE);
        tranlocal.openForWrite(LOCKMODE_NONE);

        assertFalse(tranlocal.isReadonly());
        assertEquals(initialVersion, tranlocal.version);
        assertEquals(initialValue, tranlocal.value);
        assertEquals(initialValue, tranlocal.oldValue);
        assertNull(ref.___getLockOwner());
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }
}
