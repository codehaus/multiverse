package org.multiverse.stms.beta.integrationtest.isolation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.api.exceptions.ReadConflict;
import org.multiverse.api.exceptions.WriteConflict;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;

public class WriteSkewTest {
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
    public void whenWriteSkewAllowed_thenNotDetected() {
        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransaction tx = stm.createTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setWriteSkewAllowed(true)
                .build()
                .start();

        ref1.incrementAndGet(tx, 1);
        ref2.get(tx);

        ref2.atomicIncrementAndGet(1);

        tx.commit();
        assertEquals(1, ref1.atomicGet());
    }

    @Test
    public void whenPrivatizeWritesPessimisticLockLevel_thenWriteSkewNotDetected() {
        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransaction tx = stm.createTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setWriteSkewAllowed(true)
                .setPessimisticLockLevel(PessimisticLockLevel.PrivatizeWrites)
                .build()
                .start();

        ref1.incrementAndGet(tx, 1);
        ref2.get(tx);

        ref2.atomicIncrementAndGet(1);

        tx.commit();
        assertEquals(1, ref1.atomicGet());
    }

    @Test
    public void whenEnsureWritesPessimisticLockLevel_thenWriteSkewNotDetected() {
        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransaction tx = stm.createTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setWriteSkewAllowed(true)
                .setPessimisticLockLevel(PessimisticLockLevel.EnsureWrites)
                .build()
                .start();

        ref1.incrementAndGet(tx, 1);
        ref2.get(tx);

        ref2.atomicIncrementAndGet(1);

        tx.commit();
        assertEquals(1, ref1.atomicGet());
    }

     @Test
    public void whenEnsureReadsPessimisticLockLevel_thenWriteSkewNotPossible() {
        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransaction tx = stm.createTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setWriteSkewAllowed(true)
                .setPessimisticLockLevel(PessimisticLockLevel.EnsureReads)
                .build()
                .start();

        ref1.incrementAndGet(tx, 1);
        ref2.get(tx);

        BetaTransaction otherTx = stm.startDefaultTransaction();
         ref2.incrementAndGet(otherTx, 1);

        try {
            otherTx.commit();
            fail();
        } catch (WriteConflict expected) {
        }

        tx.commit();
    }

    @Test
    public void whenPrivatizedReadsLockLevel_thenWriteSkewNotPossible() {
        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransaction tx = stm.createTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setWriteSkewAllowed(true)
                .setPessimisticLockLevel(PessimisticLockLevel.PrivatizeReads)
                .build()
                .start();

        ref1.incrementAndGet(tx, 1);
        ref2.get(tx);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        try {
            ref2.incrementAndGet(otherTx, 1);
            fail();
        } catch (ReadConflict expected) {
        }

        tx.commit();
    }

    @Test
    public void whenPrivatized_thenWriteSkewNotPossible() {
        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransaction tx = stm.createTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setWriteSkewAllowed(false)
                .build()
                .start();

        ref1.incrementAndGet(tx, 1);
        ref2.privatize(tx);
        ref2.get(tx);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        try {
            ref2.incrementAndGet(otherTx, 1);
            fail();
        } catch (ReadConflict expected) {

        }

        tx.commit();
    }

    @Test
     public void whenEnsured_thenWriteSkewNotPossible() {
         BetaLongRef ref1 = newLongRef(stm);
         BetaLongRef ref2 = newLongRef(stm);

         BetaTransaction tx = stm.createTransactionFactoryBuilder()
                 .setSpeculativeConfigurationEnabled(false)
                 .setWriteSkewAllowed(false)
                 .build()
                 .start();

         ref1.incrementAndGet(tx, 1);
         ref2.ensure(tx);
         ref2.get(tx);

         BetaTransaction otherTx = stm.startDefaultTransaction();
         ref2.incrementAndGet(otherTx, 1);

         try {
             otherTx.commit();
             fail();
         } catch (WriteConflict expected) {
         }

         tx.commit();
     }


    @Test
    public void whenWriteSkewNotAllowed_thenDetected() {
        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransaction tx = stm.createTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setWriteSkewAllowed(false)
                .build()
                .start();
        ref1.incrementAndGet(tx, 1);
        ref2.get(tx);

        ref2.atomicIncrementAndGet(1);

        try {
            tx.commit();
            fail();
        } catch (WriteConflict expected) {
        }
    }
}
