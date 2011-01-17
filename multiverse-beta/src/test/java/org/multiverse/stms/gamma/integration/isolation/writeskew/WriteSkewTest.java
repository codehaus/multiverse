package org.multiverse.stms.gamma.integration.isolation.writeskew;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.IsolationLevel;
import org.multiverse.api.LockMode;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class WriteSkewTest {
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
    public void whenWriteSkewAllowed_thenNotDetected() {
        GammaLongRef ref1 = new GammaLongRef(stm);
        GammaLongRef ref2 = new GammaLongRef(stm);

        GammaTransaction tx = stm.newTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setIsolationLevel(IsolationLevel.Snapshot)
                .build()
                .newTransaction();

        ref1.incrementAndGet(tx, 1);
        ref2.get(tx);

        ref2.atomicIncrementAndGet(1);

        tx.commit();
        assertEquals(1, ref1.atomicGet());
    }

    @Test
    public void whenWritesLocked_thenWriteSkewNotDetected() {
        whenWritesLocked_thenWriteSkewNotDetected(LockMode.None);
        whenWritesLocked_thenWriteSkewNotDetected(LockMode.Read);
        whenWritesLocked_thenWriteSkewNotDetected(LockMode.Write);
        whenWritesLocked_thenWriteSkewNotDetected(LockMode.Exclusive);
    }

    public void whenWritesLocked_thenWriteSkewNotDetected(LockMode writeLockMode) {
        GammaLongRef ref1 = new GammaLongRef(stm);
        GammaLongRef ref2 = new GammaLongRef(stm);

        GammaTransaction tx = stm.newTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setIsolationLevel(IsolationLevel.Snapshot)
                .setWriteLockMode(writeLockMode)
                .build()
                .newTransaction();

        ref1.incrementAndGet(tx, 1);
        ref2.get(tx);

        ref2.atomicIncrementAndGet(1);

        tx.commit();
        assertEquals(1, ref1.atomicGet());
    }

    @Test
    public void whenReadsLocked_thenWriteSkewNotPossible() {
        whenReadsLocked_thenWriteSkewNotPossible(LockMode.Read);
        whenReadsLocked_thenWriteSkewNotPossible(LockMode.Write);
        whenReadsLocked_thenWriteSkewNotPossible(LockMode.Exclusive);
    }

    public void whenReadsLocked_thenWriteSkewNotPossible(LockMode readLockMode) {
        GammaLongRef ref1 = new GammaLongRef(stm);
        GammaLongRef ref2 = new GammaLongRef(stm);

        GammaTransaction tx = stm.newTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setIsolationLevel(IsolationLevel.Snapshot)
                .setReadLockMode(readLockMode)
                .build()
                .newTransaction();

        ref1.incrementAndGet(tx, 1);
        ref2.get(tx);

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref2.incrementAndGet(otherTx, 1);

        try {
            otherTx.commit();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        tx.commit();
    }


    @Test
    @Ignore
    public void whenPrivatized_thenWriteSkewNotPossible() {
        GammaLongRef ref1 = new GammaLongRef(stm);
        GammaLongRef ref2 = new GammaLongRef(stm);

        GammaTransaction tx = stm.newTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setIsolationLevel(IsolationLevel.Snapshot)
                .build()
                .newTransaction();

        ref1.incrementAndGet(tx, 1);
        ref2.getLock().acquire(tx, LockMode.Exclusive);
        ref2.get(tx);

        GammaTransaction otherTx = stm.startDefaultTransaction();
        try {
            ref2.incrementAndGet(otherTx, 1);
            fail();
        } catch (ReadWriteConflict expected) {

        }

        tx.commit();
    }

    @Test
    @Ignore
    public void whenEnsured_thenWriteSkewNotPossible() {
        GammaLongRef ref1 = new GammaLongRef(stm);
        GammaLongRef ref2 = new GammaLongRef(stm);

        GammaTransaction tx = stm.newTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setIsolationLevel(IsolationLevel.Snapshot)
                .build()
                .newTransaction();

        ref1.incrementAndGet(tx, 1);
        ref2.getLock().acquire(tx, LockMode.Write);
        ref2.get(tx);

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref2.incrementAndGet(otherTx, 1);

        try {
            otherTx.commit();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        tx.commit();
    }

    @Test
    public void whenSerializedIsolationLevel_thenWriteSkewNotPossible() {
        GammaLongRef ref1 = new GammaLongRef(stm);
        GammaLongRef ref2 = new GammaLongRef(stm);

        GammaTransaction tx = stm.newTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setIsolationLevel(IsolationLevel.Serializable)
                .build()
                .newTransaction();
        ref1.incrementAndGet(tx, 1);
        ref2.get(tx);

        ref2.atomicIncrementAndGet(1);

        try {
            tx.commit();
            fail();
        } catch (ReadWriteConflict expected) {
        }
    }
}
