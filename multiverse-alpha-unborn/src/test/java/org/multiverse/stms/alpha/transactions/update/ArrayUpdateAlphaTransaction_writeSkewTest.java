package org.multiverse.stms.alpha.transactions.update;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.WriteSkewConflict;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRefTranlocal;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsCommitted;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * There were some problems getting the writeskew detection under stress to work.
 */
public class ArrayUpdateAlphaTransaction_writeSkewTest {

    private AlphaStmConfig stmConfig;
    private AlphaStm stm;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    public AlphaTransaction startSutTransaction(int size, boolean allowWriteSkewProblem) {
        UpdateConfiguration config = new UpdateConfiguration(stmConfig.clock)
                .withAutomaticReadTrackingEnabled(true)
                .withWriteSkewProblemAllowed(allowWriteSkewProblem);

        return new ArrayUpdateAlphaTransaction(config, size);
    }

    @Test
    public void testSettings() {
        AlphaTransaction tx1 = startSutTransaction(10, true);
        assertTrue(tx1.getConfiguration().isWriteSkewProblemAllowed());
        assertTrue(tx1.getConfiguration().isAutomaticReadTrackingEnabled());

        AlphaTransaction tx2 = startSutTransaction(10, false);
        assertFalse(tx2.getConfiguration().isWriteSkewProblemAllowed());
        assertTrue(tx2.getConfiguration().isAutomaticReadTrackingEnabled());
    }

    @Test
    public void whenDisallowedWriteSkewProblem_thenWriteSkewConflict() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRefTranlocal committedRef1 = (ManualRefTranlocal) ref1.___load();
        ManualRef ref2 = new ManualRef(stm);
        ManualRefTranlocal committedRef2 = (ManualRefTranlocal) ref2.___load();

        AlphaTransaction tx1 = startSutTransaction(10, false);
        tx1.openForRead(ref1);
        ManualRefTranlocal tranlocalRef2 = (ManualRefTranlocal) tx1.openForWrite(ref2);
        tranlocalRef2.value++;

        AlphaTransaction tx2 = startSutTransaction(10, false);
        tx2.openForRead(ref2);
        ManualRefTranlocal tranlocalRef1 = (ManualRefTranlocal) tx2.openForWrite(ref1);
        tranlocalRef1.value++;

        tx1.commit();
        long version = stm.getVersion();

        try {
            tx2.commit();
            fail();
        } catch (WriteSkewConflict expected) {
        }

        assertIsAborted(tx2);
        assertEquals(version + 1, stm.getVersion());
        assertSame(committedRef1, ref1.___load());
        assertSame(tranlocalRef2, ref2.___load());
    }

    @Test
    public void whenEnabledWriteSkewProblem_writeSkewProblemHappens() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);

        AlphaTransaction tx1 = startSutTransaction(10, true);
        tx1.openForRead(ref1);
        ManualRefTranlocal tranlocalRef2 = (ManualRefTranlocal) tx1.openForWrite(ref2);
        tranlocalRef2.value++;

        AlphaTransaction tx2 = startSutTransaction(10, true);
        tx2.openForRead(ref2);
        ManualRefTranlocal tranlocalRef1 = (ManualRefTranlocal) tx2.openForWrite(ref1);
        tranlocalRef1.value++;

        tx1.commit();

        long version = stm.getVersion();
        tx2.commit();

        assertIsCommitted(tx2);
        assertEquals(version + 1, stm.getVersion());
        assertSame(tranlocalRef1, ref1.___load());
        assertSame(tranlocalRef2, ref2.___load());
    }

    @Test
    public void withFourAccountsAndAllowWriteSkewProblem() {
        ManualRef accountA1 = new ManualRef(stm);
        ManualRef accountA2 = new ManualRef(stm);

        ManualRef accountB1 = new ManualRef(stm);
        ManualRef accountB2 = new ManualRef(stm);

        accountA1.set(stm, 50);
        accountB1.set(stm, 50);

        AlphaTransaction tx1 = startSutTransaction(10, true);
        if (accountA1.get(tx1) + accountA2.get(tx1) > 25) {
            accountA1.inc(tx1, -25);
            accountB1.inc(tx1, 25);
        }

        AlphaTransaction tx2 = startSutTransaction(10, true);
        if (accountB1.get(tx2) + accountB2.get(tx2) > 25) {
            accountB2.inc(tx2, -25);
            accountA2.inc(tx2, 25);
        }

        tx1.commit();
        tx2.commit();
    }


    @Test
    public void withFourAccountsAndDisallowedWriteSkewProblem_thenWriteSkewConflict() {
        ManualRef accountA1 = new ManualRef(stm);
        ManualRef accountA2 = new ManualRef(stm);

        ManualRef accountB1 = new ManualRef(stm);
        ManualRef accountB2 = new ManualRef(stm);

        accountA1.set(stm, 50);
        accountB1.set(stm, 50);

        AlphaTransaction tx1 = startSutTransaction(10, false);
        if (accountA1.get(tx1) + accountA2.get(tx1) > 25) {
            accountA1.inc(tx1, -25);
            accountB1.inc(tx1, 25);
        }

        AlphaTransaction tx2 = startSutTransaction(10, false);
        if (accountB1.get(tx2) + accountB2.get(tx2) > 25) {
            accountB2.inc(tx2, -25);
            accountA2.inc(tx2, 25);
        }

        tx1.commit();

        try {
            tx2.commit();
            fail();
        } catch (WriteSkewConflict expected) {
        }
    }


}