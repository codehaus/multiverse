package org.multiverse.stms.beta.integrationtest.isolation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.WriteConflict;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.assertAborted;
import static org.multiverse.TestUtils.assertCommitted;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

public class WriteConflictTest {

    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void whenDirtyCheckAndChange_ThenWriteConflict() {
        BetaLongRef ref = createLongRef(stm);

        BetaTransaction tx = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigEnabled(false)
                .setDirtyCheckEnabled(true)
                .build()
                .start();
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        write.value++;

        BetaTransaction otherTx = stm.startDefaultTransaction();
        LongRefTranlocal conflictingWrite = otherTx.openForWrite(ref, false, pool);
        conflictingWrite.value++;
        otherTx.commit();

        try {
            tx.commit();
            fail();
        } catch (WriteConflict expected) {

        }

        assertAborted(tx);
        assertSame(conflictingWrite, ref.___unsafeLoad());
    }

    @Test
    public void whenDirtyCheckAndNoChange_ThenNoWriteConflict() {
        BetaLongRef ref = createLongRef(stm);

        BetaTransaction tx = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigEnabled(false)
                .setDirtyCheckEnabled(true)
                .build()
                .start();
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        LongRefTranlocal conflictingWrite = otherTx.openForWrite(ref, false, pool);
        conflictingWrite.value++;
        otherTx.commit();

        tx.commit();

        assertCommitted(tx);
        assertSame(conflictingWrite, ref.___unsafeLoad());
    }


    @Test
    public void whenNoDirtyCheckAndChange_ThenWriteConflict() {
        BetaLongRef ref = createLongRef(stm);

        BetaTransaction tx = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigEnabled(false)
                .setDirtyCheckEnabled(false)
                .build()
                .start();
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        write.value++;

        BetaTransaction otherTx = stm.startDefaultTransaction();
        LongRefTranlocal conflictingWrite = otherTx.openForWrite(ref, false, pool);
        conflictingWrite.value++;
        otherTx.commit();

        try {
            tx.commit();
            fail();
        } catch (WriteConflict expected) {
        }

        assertAborted(tx);
        assertSame(conflictingWrite, ref.___unsafeLoad());
    }

    @Test
    public void whenNoDirtyCheckAndNoChange_ThenWriteConflict() {
        BetaLongRef ref = createLongRef(stm);

        BetaTransaction tx = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigEnabled(false)
                .setDirtyCheckEnabled(false)
                .build()
                .start();
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        write.value++;

        BetaTransaction otherTx = stm.startDefaultTransaction();
        LongRefTranlocal conflictingWrite = otherTx.openForWrite(ref, false, pool);
        conflictingWrite.value++;
        otherTx.commit();

        try {
            tx.commit();
            fail();
        } catch (WriteConflict expected) {
        }

        assertAborted(tx);
        assertSame(conflictingWrite, ref.___unsafeLoad());
    }
}
