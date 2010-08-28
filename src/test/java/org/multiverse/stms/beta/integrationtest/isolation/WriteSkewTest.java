package org.multiverse.stms.beta.integrationtest.isolation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.api.exceptions.ReadConflict;
import org.multiverse.api.exceptions.WriteConflict;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

public class WriteSkewTest {
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
    public void whenWriteSkewAllowed_thenNotDetected() {
        BetaLongRef ref1 = createLongRef(stm);
        BetaLongRef ref2 = createLongRef(stm);

        BetaTransaction tx = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigEnabled(false)
                .setWriteSkewAllowed(true)
                .build()
                .start();

        tx.openForWrite(ref1, false, pool).value++;
        tx.openForRead(ref2, false, pool);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForWrite(ref2, false, pool).value++;
        otherTx.commit();

        tx.commit(pool);
        assertEquals(1, ref1.___unsafeLoad().value);
    }

    @Test
    public void whenLockReadsPessimisticLockLevel_thenWriteSkewNotDetected() {
        BetaLongRef ref1 = createLongRef(stm);
        BetaLongRef ref2 = createLongRef(stm);

        BetaTransaction tx = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigEnabled(false)
                .setWriteSkewAllowed(true)
                .setPessimisticLockLevel(PessimisticLockLevel.Write)
                .build()
                .start();

        tx.openForWrite(ref1, false, pool).value++;
        tx.openForRead(ref2, false, pool);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForWrite(ref2, false, pool).value++;
        otherTx.commit();

        tx.commit(pool);
        assertEquals(1, ref1.___unsafeLoad().value);
    }

    @Test
    public void whenLockReadsPessimisticLockLevel_thenWriteSkewNotPossible() {
        BetaLongRef ref1 = createLongRef(stm);
        BetaLongRef ref2 = createLongRef(stm);

        BetaTransaction tx = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigEnabled(false)
                .setWriteSkewAllowed(true)
                .setPessimisticLockLevel(PessimisticLockLevel.Read)
                .build()
                .start();

        tx.openForWrite(ref1, true, pool).value++;
        tx.openForRead(ref2, true, pool);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        try {
            otherTx.openForWrite(ref2, false, pool).value++;
            fail();
        } catch (ReadConflict expected) {

        }

        tx.commit(pool);
    }

    @Test
    public void whenPessimisticLockingUsed_thenWriteSkewNotPossible() {
        BetaLongRef ref1 = createLongRef(stm);
        BetaLongRef ref2 = createLongRef(stm);

        BetaTransaction tx = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigEnabled(false)
                .setWriteSkewAllowed(false)
                .build()
                .start();

        tx.openForWrite(ref1, true, pool).value++;
        tx.openForRead(ref2, true, pool);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        try {
            otherTx.openForWrite(ref2, false, pool).value++;
            fail();
        } catch (ReadConflict expected) {

        }

        tx.commit(pool);
    }

    @Test
    public void whenWriteSkewNotAllowed_thenDetected() {
        BetaLongRef ref1 = createLongRef(stm);
        BetaLongRef ref2 = createLongRef(stm);

        BetaTransaction tx = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigEnabled(false)
                .setWriteSkewAllowed(false)
                .build()
                .start();

        tx.openForWrite(ref1, false, pool).value++;
        tx.openForRead(ref2, false, pool);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForWrite(ref2, false, pool).value++;
        otherTx.commit();

        try {
            tx.commit(pool);
            fail();
        } catch (WriteConflict expected) {

        }
    }
}
