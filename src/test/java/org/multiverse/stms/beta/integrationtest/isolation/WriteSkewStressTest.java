package org.multiverse.stms.beta.integrationtest.isolation;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicClosure;
import org.multiverse.api.closures.AtomicIntClosure;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.IntRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.String.format;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.createIntRef;
import static org.multiverse.stms.beta.ThreadLocalBetaObjectPool.getThreadLocalBetaObjectPool;

public class WriteSkewStressTest {
    private volatile boolean stop;
    private User user1;
    private User user2;

    enum Mode {
        normal,
        pessimisticLockLevelRead,
        pessimisticLockLevelWrite,
        pessimisticRead,
        pessimisticWrite,
        withoutWriteSkew
    }

    private Mode mode;
    private TransferThread[] threads;
    private AtomicBoolean writeSkewEncountered = new AtomicBoolean();
    private BetaStm stm;
    private BetaObjectPool pool;
    private int threadCount = 8;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
        clearThreadLocalTransaction();
        user1 = new User();
        user2 = new User();
        stop = false;
        writeSkewEncountered.set(false);

        threads = new TransferThread[threadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new TransferThread(k);
        }

        IntRef account = user1.getRandomAccount();
        BetaTransaction tx = stm.startDefaultTransaction();
        tx.openForWrite(account, false, pool).value = 1000;
        tx.commit();
    }

    @Test
    public void whenPessimisticRead_thenNoWriteSkewPossible() {
        mode = Mode.pessimisticRead;
        startAll(threads);
        sleepMs(getStressTestDurationMs(30 * 1000));
        stop = true;

        joinAll(threads);

        System.out.println("User1: " + user1);
        System.out.println("User2: " + user2);

        assertFalse("writeskew detected", writeSkewEncountered.get());
    }

    @Test
    public void whenPessimisticWrite_thenWriteSkewPossible() {
        mode = Mode.pessimisticWrite;
        startAll(threads);
        sleepMs(getStressTestDurationMs(30 * 1000));
        stop = true;

        joinAll(threads);

        System.out.println("User1: " + user1);
        System.out.println("User2: " + user2);

        assertTrue("writeskew detected", writeSkewEncountered.get());
    }

    @Test
    public void whenPessimisticLockLevelWrite_thenWriteSkewPossible() {
        mode = Mode.pessimisticLockLevelWrite;
        startAll(threads);
        sleepMs(getStressTestDurationMs(30 * 1000));
        stop = true;

        joinAll(threads);

        System.out.println("User1: " + user1);
        System.out.println("User2: " + user2);

        assertTrue("writeskew detected", writeSkewEncountered.get());
    }

    @Test
    public void whenPessimisticLockLevelRead_thenNoWriteSkewPossible() {
        mode = Mode.pessimisticLockLevelRead;
        startAll(threads);
        sleepMs(getStressTestDurationMs(30 * 1000));
        stop = true;

        joinAll(threads);

        System.out.println("User1: " + user1);
        System.out.println("User2: " + user2);

        assertFalse("writeskew detected", writeSkewEncountered.get());
    }

    @Test
    public void whenWriteSkewAllowed_thenWriteSkewPossible() {
        mode = Mode.normal;
        startAll(threads);
        sleepMs(getStressTestDurationMs(30 * 1000));
        stop = true;
        joinAll(threads);

        System.out.println("User1: " + user1);
        System.out.println("User2: " + user2);

        System.out.println("User1.account1: " + user1.account1.___toOrecString());
        System.out.println("User1.account2: " + user1.account2.___toOrecString());

        System.out.println("User2.account1: " + user2.account1.___toOrecString());
        System.out.println("User2.account2: " + user2.account2.___toOrecString());

        assertTrue(writeSkewEncountered.get());
    }

    @Test
    public void whenWriteSkewNotAllowed_thenNoWriteSkewPossible() {
        mode = Mode.withoutWriteSkew;
        startAll(threads);
        sleepMs(getStressTestDurationMs(30 * 1000));
        stop = true;

        joinAll(threads);

        System.out.println("User1: " + user1);
        System.out.println("User2: " + user2);

        assertFalse("writeskew detected", writeSkewEncountered.get());
    }

    public class TransferThread extends TestThread {

        private final AtomicBlock normal = stm.getTransactionFactoryBuilder()
                .setWriteSkewAllowed(true)
                .setMaxRetries(10000)
                .buildAtomicBlock();
        private final AtomicBlock noWriteSkew = stm.getTransactionFactoryBuilder()
                .setWriteSkewAllowed(false)
                .setMaxRetries(10000)
                .buildAtomicBlock();
        private final AtomicBlock pessimisticReadLockLevel = stm.getTransactionFactoryBuilder()
                .setPessimisticLockLevel(PessimisticLockLevel.Read)
                .setWriteSkewAllowed(true)
                .setMaxRetries(10000)
                .buildAtomicBlock();
        private final AtomicBlock pessimisticWriteLockLevel = stm.getTransactionFactoryBuilder()
                .setPessimisticLockLevel(PessimisticLockLevel.Write)
                .setWriteSkewAllowed(true)
                .setMaxRetries(10000)
                .buildAtomicBlock();


        public TransferThread(int id) {
            super("TransferThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            int k = 0;
            while (!stop) {
                if (k % 10000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }

                switch (mode) {
                    case normal:
                        doItNormal();
                        break;
                    case withoutWriteSkew:
                        doItWithNoWriteSkew();
                        break;
                    case pessimisticLockLevelRead:
                        doItWithPessimisticReadLockLevel();
                        break;
                    case pessimisticLockLevelWrite:
                        doItWithPessimisticWriteLockLevel();
                        break;
                    case pessimisticRead:
                        doItPessimisticRead();
                        break;
                    case pessimisticWrite:
                        doItPessimisticWrite();
                        break;
                    default:
                        throw new IllegalStateException();
                }

                k++;
            }
        }

        private void doItWithPessimisticReadLockLevel() {
            pessimisticReadLockLevel.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    BetaObjectPool pool = getThreadLocalBetaObjectPool();
                    doIt(btx, pool, false, false);
                }
            });
        }

        private void doItWithPessimisticWriteLockLevel() {
            pessimisticWriteLockLevel.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    BetaObjectPool pool = getThreadLocalBetaObjectPool();
                    doIt(btx, pool, false, false);
                }
            });
        }

        private void doItWithNoWriteSkew() {
            noWriteSkew.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    BetaObjectPool pool = getThreadLocalBetaObjectPool();
                    doIt(btx, pool, false, false);
                }
            });
        }

        private void doItNormal() {
            normal.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    BetaObjectPool pool = getThreadLocalBetaObjectPool();
                    doIt(btx, pool, false, false);
                }
            });
        }

        private void doItPessimisticRead() {
            normal.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    BetaObjectPool pool = getThreadLocalBetaObjectPool();
                    doIt(btx, pool, true, true);
                }
            });
        }

        private void doItPessimisticWrite() {
            normal.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    BetaObjectPool pool = getThreadLocalBetaObjectPool();
                    doIt(btx, pool, false, true);
                }
            });
        }

        public void doIt(BetaTransaction tx, BetaObjectPool pool, boolean pessimisticRead, boolean pessimisticWrite) {
            int amount = randomInt(100);

            User from = random(user1, user2);
            User to = random(user1, user2);

            int sum = tx.openForRead(from.account1, pessimisticRead, pool).value
                    + tx.openForRead(from.account2, pessimisticRead, pool).value;

            if (sum < 0) {
                if (!writeSkewEncountered.get()) {
                    writeSkewEncountered.set(true);
                    System.out.println("writeskew detected");
                }
            }

            if (sum >= amount) {
                IntRef fromAccount = from.getRandomAccount();
                tx.openForWrite(fromAccount, pessimisticWrite, pool).value -= amount;

                IntRef toAccount = to.getRandomAccount();
                tx.openForWrite(toAccount, pessimisticWrite, pool).value += amount;
            }

            sleepRandomUs(1000);
        }
    }

    public User random(User user1, User user2) {
        return randomBoolean() ? user1 : user2;
    }

    public class User {
        private AtomicBlock getTotalBlock = stm.getTransactionFactoryBuilder()
                .setReadonly(true)
                .buildAtomicBlock();

        private IntRef account1 = createIntRef(stm);
        private IntRef account2 = createIntRef(stm);

        public IntRef getRandomAccount() {
            return randomBoolean() ? account1 : account2;
        }

        public int getTotal() {
            return getTotalBlock.execute(new AtomicIntClosure() {
                @Override
                public int execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    BetaObjectPool pool = getThreadLocalBetaObjectPool();
                    return account1.get(btx, pool) + account2.get(btx, pool);
                }
            });
        }

        public String toString() {
            return stm.getTransactionFactoryBuilder().buildAtomicBlock().execute(new AtomicClosure<String>() {
                @Override
                public String execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    BetaObjectPool pool = getThreadLocalBetaObjectPool();

                    return format("User(account1 = %s, account2 = %s)",
                            account1.get(btx, pool), account2.get(btx, pool));
                }
            });
        }
    }
}
