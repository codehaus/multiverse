package org.multiverse.stms.beta.integrationtest.isolation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.WriteConflict;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsCommitted;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;

public class WriteConflictTest {

    private BetaStm stm;
  
    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = new BetaStm();
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
                .start();
        LongRefTranlocal write = tx.openForWrite(ref, false);
        write.value++;

        BetaTransaction otherTx = stm.startDefaultTransaction();
        LongRefTranlocal conflictingWrite = otherTx.openForWrite(ref, false);
        conflictingWrite.value++;
        otherTx.commit();

        try {
            tx.commit();
            fail();
        } catch (WriteConflict expected) {

        }

        assertIsAborted(tx);
        assertSame(conflictingWrite, ref.___unsafeLoad());
    }

    @Test
    public void whenDirtyCheckAndNoChange_ThenNoWriteConflict() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.createTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setDirtyCheckEnabled(true)
                .build()
                .start();
        LongRefTranlocal write = tx.openForWrite(ref, false);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        LongRefTranlocal conflictingWrite = otherTx.openForWrite(ref, false);
        conflictingWrite.value++;
        otherTx.commit();

        tx.commit();

        assertIsCommitted(tx);
        assertSame(conflictingWrite, ref.___unsafeLoad());
    }


    @Test
    public void whenNoDirtyCheckAndChange_ThenWriteConflict() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.createTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setDirtyCheckEnabled(false)
                .build()
                .start();
        LongRefTranlocal write = tx.openForWrite(ref, false);
        write.value++;

        BetaTransaction otherTx = stm.startDefaultTransaction();
        LongRefTranlocal conflictingWrite = otherTx.openForWrite(ref, false);
        conflictingWrite.value++;
        otherTx.commit();

        try {
            tx.commit();
            fail();
        } catch (WriteConflict expected) {
        }

        assertIsAborted(tx);
        assertSame(conflictingWrite, ref.___unsafeLoad());
    }

    @Test
    public void whenNoDirtyCheckAndNoChange_ThenWriteConflict() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.createTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setDirtyCheckEnabled(false)
                .build()
                .start();
        LongRefTranlocal write = tx.openForWrite(ref, false);
        write.value++;

        BetaTransaction otherTx = stm.startDefaultTransaction();
        LongRefTranlocal conflictingWrite = otherTx.openForWrite(ref, false);
        conflictingWrite.value++;
        otherTx.commit();

        try {
            tx.commit();
            fail();
        } catch (WriteConflict expected) {
        }

        assertIsAborted(tx);
        assertSame(conflictingWrite, ref.___unsafeLoad());
    }
}
