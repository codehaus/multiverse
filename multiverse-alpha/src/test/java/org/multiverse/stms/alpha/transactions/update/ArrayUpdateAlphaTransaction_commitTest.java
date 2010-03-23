package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.WriteSkewConflict;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRefTranlocal;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.stms.alpha.transactions.OptimalSize;
import org.multiverse.utils.latches.CheapLatch;
import org.multiverse.utils.latches.Latch;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsCommitted;
import static org.multiverse.stms.alpha.transactions.AlphaTransactionTestUtils.assertHasNoListeners;

public class ArrayUpdateAlphaTransaction_commitTest {

    private AlphaStmConfig stmConfig;
    private AlphaStm stm;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
    }

    public AlphaTransaction startSutTransaction(int size) {
        OptimalSize optimalSize = new OptimalSize(size, size * 10);
        UpdateAlphaTransactionConfig config = new UpdateAlphaTransactionConfig(
                stmConfig.clock,
                stmConfig.backoffPolicy,
                stmConfig.commitLockPolicy,
                null,
                optimalSize,
                stmConfig.maxRetryCount, true, true, true, true, true);

        return new ArrayUpdateAlphaTransaction(config, size);
    }

    public AlphaTransaction startSutTransactionWithAllowWriteSkewProblem(int size, boolean allowWriteSkewProblem) {
        OptimalSize optimalSize = new OptimalSize(size, size + 1);
        UpdateAlphaTransactionConfig config = new UpdateAlphaTransactionConfig(
                stmConfig.clock,
                stmConfig.backoffPolicy,
                stmConfig.commitLockPolicy,
                null,
                optimalSize,
                stmConfig.maxRetryCount, true, true, allowWriteSkewProblem, true, true);

        return new ArrayUpdateAlphaTransaction(config, size);
    }


    @Test
    public void whenUnused_thenCommitSuccess() {
        AlphaTransaction tx = startSutTransaction(2);
        tx.commit();

        long version = stm.getVersion();
        tx.commit();
        assertEquals(version, stm.getVersion());
        assertIsCommitted(tx);
    }

    @Test
    public void whenFresh() {
        ManualRef txObject = ManualRef.createUncommitted();

        AlphaTransaction tx = startSutTransaction(2);
        AlphaTranlocal tranlocal = tx.openForWrite(txObject);

        long version = stm.getVersion();
        tx.commit();

        assertEquals(version + 1, stm.getVersion());
        assertIsCommitted(tx);
        assertSame(tranlocal, txObject.___load());
        assertSame(stm.getVersion(), tranlocal.getWriteVersion());
        assertSame(txObject, tranlocal.getTransactionalObject());
        assertNull(txObject.___getLockOwner());
    }

    @Test
    public void whenDirty() {
        ManualRef txObject = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction(2);
        ManualRefTranlocal tranlocal = (ManualRefTranlocal) tx.openForWrite(txObject);
        tranlocal.value++;

        long version = stm.getVersion();
        tx.commit();

        assertEquals(version + 1, stm.getVersion());
        assertIsCommitted(tx);
        assertSame(tranlocal, txObject.___load());
        assertSame(stm.getVersion(), tranlocal.getWriteVersion());
        assertSame(txObject, tranlocal.getTransactionalObject());
        assertNull(txObject.___getLockOwner());
    }

    @Test
    public void whenListenersRegistered_theyAreRemovedAndNotified() {
        ManualRef ref = new ManualRef(stm);

        Latch latch1 = new CheapLatch();
        Latch latch2 = new CheapLatch();

        registerRetryListener(ref, latch1);
        stmConfig.clock.tick();
        registerRetryListener(ref, latch2);

        AlphaTransaction tx = startSutTransaction(10);
        ManualRefTranlocal tranlocal = (ManualRefTranlocal) tx.openForWrite(ref);
        tranlocal.value++;
        tx.commit();

        assertHasNoListeners(ref);
        assertTrue(latch1.isOpen());
        assertTrue(latch2.isOpen());
    }

    private void registerRetryListener(ManualRef ref, Latch latch) {
        AlphaTransaction listenTx = stm.getTransactionFactoryBuilder()
                .setReadonly(false)
                .setAutomaticReadTracking(true).build().start();

        listenTx.openForRead(ref);
        listenTx.registerRetryLatch(latch);
    }

    @Test
    public void complexScenario() {
        ManualRef updateRef = new ManualRef(stm, 0);
        ManualRef readonlyRef = new ManualRef(stm, 0);
        ManualRef freshRef = ManualRef.createUncommitted();

        AlphaTransaction tx = startSutTransaction(3);
        ManualRefTranlocal updateTranlocal = (ManualRefTranlocal) tx.openForWrite(updateRef);
        updateTranlocal.value++;
        tx.openForRead(readonlyRef);
        ManualRefTranlocal freshTranlocal = (ManualRefTranlocal) tx.openForWrite(freshRef);
        freshTranlocal.value++;

        long version = stm.getVersion();
        tx.commit();

        assertIsCommitted(tx);
        assertEquals(version + 1, stm.getVersion());
        assertEquals(1, updateRef.get(stm));
        assertEquals(0, readonlyRef.get(stm));
        assertEquals(1, freshRef.get(stm));
    }

    @Test
    public void whenAllowWriteSkewProblem_thenCommit() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);

        AlphaTransaction tx1 = startSutTransactionWithAllowWriteSkewProblem(10, true);
        tx1.openForRead(ref1);
        ref2.inc(tx1);

        AlphaTransaction tx2 = startSutTransactionWithAllowWriteSkewProblem(10, true);
        tx2.openForRead(ref2);
        ref1.inc(tx2);

        tx1.commit();
        tx2.commit();

        assertEquals(1, ref1.get(stm));
        assertEquals(1, ref2.get(stm));
    }

    @Test
    public void whenDisallowWriteSkewProblem_thenWriteSkewConflict() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);

        AlphaTransaction tx1 = startSutTransactionWithAllowWriteSkewProblem(10, false);
        tx1.openForRead(ref1);
        ref2.inc(tx1);

        AlphaTransaction tx2 = startSutTransactionWithAllowWriteSkewProblem(10, false);
        tx2.openForRead(ref2);
        ref1.inc(tx2);

        tx1.commit();

        try {
            tx2.commit();
            fail();
        } catch (WriteSkewConflict expected) {
        }

        assertEquals(0, ref1.get(stm));
        assertEquals(1, ref2.get(stm));
    }
}
