package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.exceptions.WriteSkewConflict;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRefTranlocal;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.stms.alpha.transactions.OptimalSize;
import org.multiverse.templates.TransactionTemplate;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;

/**
 * There were some problems getting the writeskew detection under stress to work.
 */
public class ArrayUpdateAlphaTransaction_writeSkewTest {

    private AlphaStmConfig stmConfig;
    private AlphaStm stm;
    private OptimalSize optimalSize;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
        optimalSize = new OptimalSize(1);
    }

    public AlphaTransaction startSutTransaction(int size, boolean allowWriteSkewProblem) {
        optimalSize.set(size);
        ArrayUpdateAlphaTransaction.Config config = new ArrayUpdateAlphaTransaction.Config(
                stmConfig.clock,
                stmConfig.backoffPolicy,
                null,
                stmConfig.commitLockPolicy,
                stmConfig.maxRetryCount,
                allowWriteSkewProblem,
                optimalSize,
                true, false, true, true, size
        );
        return new ArrayUpdateAlphaTransaction(config, size);
    }

    @Test
    public void testSettings() {
        AlphaTransaction tx1 = startSutTransaction(10, true);
        assertTrue(tx1.getConfig().allowWriteSkewProblem());
        assertTrue(tx1.getConfig().automaticReadTracking());

        AlphaTransaction tx2 = startSutTransaction(10, false);
        assertFalse(tx2.getConfig().allowWriteSkewProblem());
        assertTrue(tx2.getConfig().automaticReadTracking());
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

    @Test
    public void testConcurrent() {
        ManualRef account1 = new ManualRef(stm);
        ManualRef account2 = new ManualRef(stm);

        ManualRef account3 = new ManualRef(stm);
        ManualRef account4 = new ManualRef(stm);

        account1.set(stm, 1000);

        TransferThread t1 = new TransferThread(0, account1, account2, account3, account4, true);
        TransferThread t2 = new TransferThread(0, account3, account4, account1, account2, true);

        startAll(t1, t2);
        joinAll(t1, t2);

        System.out.println("account1 " + account1.get(stm));
        System.out.println("account2 " + account2.get(stm));
        System.out.println("account3 " + account3.get(stm));
        System.out.println("account4 " + account4.get(stm));

        assertTrue(account1.get(stm) + account2.get(stm) >= 0);
        assertTrue(account3.get(stm) + account4.get(stm) >= 0);
    }

    @Test
    @Ignore
    public void testConcurrentWithoutWriteSkewDetection() {
        ManualRef accountA1 = new ManualRef(stm);
        ManualRef accountA2 = new ManualRef(stm);

        ManualRef accountB1 = new ManualRef(stm);
        ManualRef accountB2 = new ManualRef(stm);

        accountA1.set(stm, 1000);
        accountB1.set(stm, 1000);

        TransferThread t1 = new TransferThread(0, accountA1, accountA2, accountB1, accountB1, false);//todo:
        TransferThread t2 = new TransferThread(0, accountB1, accountB2, accountA1, accountA1, false);//todo:

        startAll(t1, t2);
        joinAll(t1, t2);

        System.out.println("account1 " + accountA1.get(stm));
        System.out.println("account2 " + accountA2.get(stm));
        System.out.println("account3 " + accountB1.get(stm));
        System.out.println("account4 " + accountB2.get(stm));

        boolean accountAViolation = accountA1.get(stm) + accountA2.get(stm) < 0;
        boolean accountBViolation = accountB1.get(stm) + accountB2.get(stm) < 0;

        assertTrue(accountAViolation || accountBViolation);
    }


    public ManualRef random(ManualRef ref1, ManualRef ref2) {
        return randomBoolean() ? ref1 : ref2;
    }

    public class TransferThread extends TestThread {

        final ManualRef accountFrom1;
        final ManualRef accountFrom2;
        final ManualRef accountTo1;
        final ManualRef accountTo2;
        final boolean enabled;

        public TransferThread(int id, ManualRef accountFrom1, ManualRef accountFrom2, ManualRef accountTo1,
                              ManualRef accountTo2, boolean enabled) {
            super("TransferThread-" + id);

            this.accountFrom1 = accountFrom1;
            this.accountFrom2 = accountFrom2;
            this.accountTo1 = accountTo1;
            this.accountTo2 = accountTo2;
            this.enabled = enabled;
        }

        @Override
        public void doRun() throws Exception {
            TransactionFactory txFactory = new TransactionFactory() {
                @Override
                public Transaction start() {
                    return startSutTransaction(10, enabled);
                }
            };

            for (int k = 0; k < 1000; k++) {
                new TransactionTemplate(txFactory) {
                    @Override
                    public Object execute(Transaction t) throws Exception {
                        AlphaTransaction tx = (AlphaTransaction) t;
                        ManualRefTranlocal tranlocalAccountFrom1 = (ManualRefTranlocal) tx.openForRead(accountFrom1);
                        ManualRefTranlocal tranlocalAccountFrom2 = (ManualRefTranlocal) tx.openForRead(accountFrom2);

                        if (tranlocalAccountFrom1.value + tranlocalAccountFrom2.value >= 100) {
                            ManualRef from = random(accountFrom1, accountFrom2);
                            ManualRefTranlocal tranlocalFrom = (ManualRefTranlocal) tx.openForWrite(from);
                            tranlocalFrom.value -= 100;

                            ManualRef to = random(accountTo1, accountTo2);
                            ManualRefTranlocal tranlocalTo = (ManualRefTranlocal) tx.openForWrite(to);
                            tranlocalTo.value += 100;

                            assert2WritesAndOneRead(tx, accountFrom1, accountFrom2, to);
                        }

                        return null;
                    }
                }.execute();
            }
        }

        private void assert2WritesAndOneRead(AlphaTransaction tx, ManualRef from1, ManualRef from2, ManualRef to) {
            int firstFreeIndex = (Integer) getField(tx, "firstFreeIndex");
            assertEquals(3, firstFreeIndex);

            AlphaTranlocal[] attached = (AlphaTranlocal[]) getField(tx, "attachedArray");
            AlphaTranlocal from1Tranlocal = find(attached, firstFreeIndex, from1);
            AlphaTranlocal from2Tranlocal = find(attached, firstFreeIndex, from2);

            //one of them both is  readonly
            assertTrue(from1Tranlocal.isCommitted() || from2Tranlocal.isCommitted());
            assertTrue(!from1Tranlocal.isCommitted() || !from2Tranlocal.isCommitted());

            AlphaTranlocal write2 = find(attached, firstFreeIndex, to);
            assertFalse(write2.isCommitted());
        }

        public AlphaTranlocal find(AlphaTranlocal[] attached, int firstFreeIndex, ManualRef ref) {
            for (int k = 0; k < firstFreeIndex; k++) {
                if (attached[k].getTransactionalObject() == ref) {
                    return attached[k];
                }
            }
            return null;
        }
    }
}
