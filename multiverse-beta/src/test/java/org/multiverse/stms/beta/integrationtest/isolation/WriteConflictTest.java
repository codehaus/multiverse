package org.multiverse.stms.beta.integrationtest.isolation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRefTranlocal;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsCommitted;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmTestUtils.assertVersionAndValue;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;

public class WriteConflictTest implements BetaStmConstants {

    private BetaStm stm;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = (BetaStm) getGlobalStmInstance();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void whenDirtyCheckAndChange_ThenWriteConflict() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.createTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setDirtyCheckEnabled(true)
                .build()
                .newTransaction();
        BetaLongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        write.value++;

        ref.atomicIncrementAndGet(1);
        long version = ref.getVersion();

        try {
            tx.commit();
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, version, 1);
    }

    @Test
    public void whenDirtyCheckAndNoChange_ThenNoWriteConflict() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.createTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setDirtyCheckEnabled(true)
                .build()
                .newTransaction();

        tx.openForWrite(ref, LOCKMODE_NONE);

        ref.atomicIncrementAndGet(1);
        long version = ref.getVersion();

        tx.commit();

        assertIsCommitted(tx);
        assertVersionAndValue(ref, version, 1);
    }


    @Test
    public void whenNoDirtyCheckAndChange_ThenWriteConflict() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.createTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setDirtyCheckEnabled(false)
                .build()
                .newTransaction();
        BetaLongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        write.value++;

        ref.atomicIncrementAndGet(1);
        long version = ref.getVersion();

        try {
            tx.commit();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, version, 1);
    }

    @Test
    public void whenNoDirtyCheckAndNoChange_ThenWriteConflict() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.createTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setDirtyCheckEnabled(false)
                .build()
                .newTransaction();
        BetaLongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        write.value++;

        ref.atomicIncrementAndGet(1);
        long version = ref.getVersion();

        try {
            tx.commit();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, version, 1);
    }
}
