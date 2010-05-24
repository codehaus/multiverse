package org.multiverse.stms.alpha.transactions.update;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.TransactionFactoryBuilder;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRefTranlocal;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.templates.TransactionTemplate;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class ArrayUpdateAlphaTransaction_writeSkewStressTest {
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

    public AlphaTransaction createSutTransaction(int size, boolean writeSkewAllowed) {
        UpdateConfiguration config = new UpdateConfiguration(stmConfig.clock)
                .withReadTrackingEnabled(true)
                .withWriteSkewAllowed(writeSkewAllowed);

        return new ArrayUpdateAlphaTransaction(config, size);
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

    //@Test

    public void testConcurrentWithWriteSkewAllowed() {
        ManualRef accountA1 = new ManualRef(stm);
        ManualRef accountA2 = new ManualRef(stm);

        ManualRef accountB1 = new ManualRef(stm);
        ManualRef accountB2 = new ManualRef(stm);

        accountA1.set(stm, 1000);
        accountB1.set(stm, 1000);

        TransferThread thread1 = new TransferThread(0, accountA1, accountA2, accountB1, accountB1, true);//todo:
        TransferThread thread2 = new TransferThread(0, accountB1, accountB2, accountA1, accountA1, true);//todo:

        startAll(thread1, thread2);
        joinAll(thread1, thread2);

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
        final boolean writeSkewAllowed;

        public TransferThread(int id, ManualRef accountFrom1, ManualRef accountFrom2, ManualRef accountTo1,
                              ManualRef accountTo2, boolean writeSkewAllowed) {
            super("TransferThread-" + id);

            this.accountFrom1 = accountFrom1;
            this.accountFrom2 = accountFrom2;
            this.accountTo1 = accountTo1;
            this.accountTo2 = accountTo2;
            this.writeSkewAllowed = writeSkewAllowed;
        }

        @Override
        public void doRun() throws Exception {
            TransactionFactory txFactory = new TransactionFactory() {

                @Override
                public Stm getStm() {
                    return stm;
                }

                @Override
                public TransactionFactoryBuilder getTransactionFactoryBuilder() {
                    return null;  //todo
                }

                @Override
                public Transaction start() {
                    AlphaTransaction tx = create();
                    tx.start();
                    return tx;
                }

                @Override
                public AlphaTransaction create() {
                    return createSutTransaction(10, writeSkewAllowed);
                }
            };

            for (int k = 0; k < 1000; k++) {
                new TransactionTemplate(txFactory) {
                    @Override
                    public Object execute(Transaction t) throws Exception {
                        AlphaTransaction tx = (AlphaTransaction) t;
                        ManualRefTranlocal tranlocalAccountFrom1 = (ManualRefTranlocal) tx.openForRead(accountFrom1);
                        ManualRefTranlocal tranlocalAccountFrom2 = (ManualRefTranlocal) tx.openForRead(accountFrom2);

                        sleepRandomMs(100);

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
