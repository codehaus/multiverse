package org.multiverse.stms.gamma.integration.isolation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaRefTranlocal;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsCommitted;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.gamma.GammaTestUtils.assertVersionAndValue;

public class WriteConflictTest implements BetaStmConstants {

    private GammaStm stm;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = (GammaStm) getGlobalStmInstance();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void whenDirtyCheckAndChange_ThenWriteConflict() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = stm.newTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setDirtyCheckEnabled(true)
                .build()
                .newTransaction();
        GammaRefTranlocal write = ref.openForWrite(tx, LOCKMODE_NONE);
        write.long_value++;

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
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = stm.newTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setDirtyCheckEnabled(true)
                .build()
                .newTransaction();

        ref.openForWrite(tx,LOCKMODE_NONE);

        ref.atomicIncrementAndGet(1);
        long version = ref.getVersion();

        tx.commit();

        assertIsCommitted(tx);
        assertVersionAndValue(ref, version, 1);
    }


    @Test
    public void whenNoDirtyCheckAndChange_ThenWriteConflict() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = stm.newTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setDirtyCheckEnabled(false)
                .build()
                .newTransaction();
        GammaRefTranlocal write = ref.openForWrite(tx, LOCKMODE_NONE);
        write.long_value++;

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
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = stm.newTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setDirtyCheckEnabled(false)
                .build()
                .newTransaction();
        GammaRefTranlocal write = ref.openForWrite(tx, LOCKMODE_NONE);
        write.long_value++;

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
