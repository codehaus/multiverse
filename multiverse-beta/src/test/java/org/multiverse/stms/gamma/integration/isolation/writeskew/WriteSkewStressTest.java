package org.multiverse.stms.gamma.integration.isolation.writeskew;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.IsolationLevel;
import org.multiverse.api.LockLevel;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicClosure;
import org.multiverse.api.closures.AtomicLongClosure;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.String.format;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * A Test that checks if the writeskew problem is happening. When
 * pessimisticRead/LockLevel.Read/writeskew=false is used, no writeskew is possible. Otherwise
 * it can happen.
 *
 * @author Peter Veentjer.
 */
public class WriteSkewStressTest {
    private volatile boolean stop;
    private User user1;
    private User user2;

    enum Mode {
        snapshot,
        privatizedReadLevel,
        privatizedWriteLevel,
        privatizedRead,
        privatizedWrite,
        serialized
    }

    private Mode mode;
    private TransferThread[] threads;
    private AtomicBoolean writeSkewEncountered = new AtomicBoolean();
    private GammaStm stm;
    private int threadCount = 8;

    @Before
    public void setUp() {
        stm = (GammaStm) getGlobalStmInstance();
        clearThreadLocalTransaction();
        user1 = new User();
        user2 = new User();
        stop = false;
        writeSkewEncountered.set(false);

        threads = new TransferThread[threadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new TransferThread(k);
        }

        GammaLongRef account = user1.getRandomAccount();
        GammaTransaction tx = stm.startDefaultTransaction();
        account.openForWrite(tx, LOCKMODE_NONE).long_value = 1000;
        tx.commit();
    }

    @Test
    public void whenPessimisticRead_thenNoWriteSkewPossible() {
        mode = Mode.privatizedRead;
        startAll(threads);
        sleepMs(getStressTestDurationMs(30 * 1000));
        stop = true;

        joinAll(threads);

        System.out.println("User1: " + user1);
        System.out.println("User2: " + user2);

        assertFalse("writeskew detected", writeSkewEncountered.get());
    }

    /**
     * If this test fails, the anomaly we are looking for, hasn't occurred yet. Try increasing the
     * running time.
     */
    @Test
    public void whenPessimisticWrite_thenWriteSkewPossible() {
        mode = Mode.privatizedWrite;
        startAll(threads);
        sleepMs(getStressTestDurationMs(30 * 1000));
        stop = true;

        joinAll(threads);

        System.out.println("User1: " + user1);
        System.out.println("User2: " + user2);

        assertTrue("no writeskew detected", writeSkewEncountered.get());
    }

    @Test
    public void whenLockLevelWrite_thenWriteSkewPossible() {
        mode = Mode.privatizedWriteLevel;
        startAll(threads);
        sleepMs(getStressTestDurationMs(30 * 1000));
        stop = true;

        joinAll(threads);

        System.out.println("User1: " + user1);
        System.out.println("User2: " + user2);

        assertTrue("no writeskew detected", writeSkewEncountered.get());
    }

    @Test
    public void whenLockLevelRead_thenNoWriteSkewPossible() {
        mode = Mode.privatizedReadLevel;
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
        mode = Mode.snapshot;
        startAll(threads);
        sleepMs(getStressTestDurationMs(30 * 1000));
        stop = true;
        joinAll(threads);

        System.out.println("User1: " + user1);
        System.out.println("User2: " + user2);

        System.out.println("User1.account1: " + user1.account1.toDebugString());
        System.out.println("User1.account2: " + user1.account2.toDebugString());

        System.out.println("User2.account1: " + user2.account1.toDebugString());
        System.out.println("User2.account2: " + user2.account2.toDebugString());

        assertTrue(writeSkewEncountered.get());
    }

    @Test
    public void whenWriteSkewNotAllowed_thenNoWriteSkewPossible() {
        mode = Mode.serialized;
        startAll(threads);
        sleepMs(getStressTestDurationMs(30 * 1000));
        stop = true;

        joinAll(threads);

        System.out.println("User1: " + user1);
        System.out.println("User2: " + user2);

        assertFalse("writeskew detected", writeSkewEncountered.get());
    }

    public class TransferThread extends TestThread {

        private final AtomicBlock snapshotBlock = stm.newTransactionFactoryBuilder()
                .setIsolationLevel(IsolationLevel.Snapshot)
                .setMaxRetries(10000)
                .newAtomicBlock();
        private final AtomicBlock serializedBlock = stm.newTransactionFactoryBuilder()
                .setIsolationLevel(IsolationLevel.Serializable)
                .setMaxRetries(10000)
                .newAtomicBlock();
        private final AtomicBlock privatizedReadLevelBlock = stm.newTransactionFactoryBuilder()
                .setLockLevel(LockLevel.CommitLockReads)
                .setIsolationLevel(IsolationLevel.Snapshot)
                .setMaxRetries(10000)
                .newAtomicBlock();
        private final AtomicBlock privatizedWriteLevelBlock = stm.newTransactionFactoryBuilder()
                .setLockLevel(LockLevel.CommitLockWrites)
                .setIsolationLevel(IsolationLevel.Snapshot)
                .setMaxRetries(10000)
                .newAtomicBlock();


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
                    case snapshot:
                        doItWithSnapshot();
                        break;
                    case serialized:
                        doItWithSerializedBlock();
                        break;
                    case privatizedReadLevel:
                        doItWithPrivatizedReadLevel();
                        break;
                    case privatizedWriteLevel:
                        doItWithPrivatizedWriteLevel();
                        break;
                    case privatizedRead:
                        doItWithPrivatizedRead();
                        break;
                    case privatizedWrite:
                        doItWithPrivatizedWrite();
                        break;
                    default:
                        throw new IllegalStateException();
                }

                k++;
            }
        }

        private void doItWithPrivatizedReadLevel() {
            privatizedReadLevelBlock.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    GammaTransaction btx = (GammaTransaction) tx;
                    doIt(btx, false, false);
                }
            });
        }

        private void doItWithPrivatizedWriteLevel() {
            privatizedWriteLevelBlock.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    GammaTransaction btx = (GammaTransaction) tx;
                    doIt(btx, false, false);
                }
            });
        }

        private void doItWithSerializedBlock() {
            serializedBlock.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    GammaTransaction btx = (GammaTransaction) tx;
                    doIt(btx, false, false);
                }
            });
        }

        private void doItWithSnapshot() {
            snapshotBlock.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    GammaTransaction btx = (GammaTransaction) tx;
                    doIt(btx, false, false);
                }
            });
        }

        private void doItWithPrivatizedRead() {
            snapshotBlock.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    GammaTransaction btx = (GammaTransaction) tx;
                    doIt(btx, true, true);
                }
            });
        }

        private void doItWithPrivatizedWrite() {
            snapshotBlock.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    GammaTransaction btx = (GammaTransaction) tx;
                    doIt(btx, false, true);
                }
            });
        }

        public void doIt(GammaTransaction tx, boolean pessimisticRead, boolean pessimisticWrite) {
            int amount = randomInt(100);

            User from = random(user1, user2);
            User to = random(user1, user2);

            long sum = from.account1.openForRead(tx, pessimisticRead ? LOCKMODE_EXCLUSIVE : LOCKMODE_NONE).long_value
                    + from.account2.openForRead(tx, pessimisticRead ? LOCKMODE_EXCLUSIVE : LOCKMODE_NONE).long_value;

            if (sum < 0) {
                if (!writeSkewEncountered.get()) {
                    writeSkewEncountered.set(true);
                    System.out.println("writeskew detected");
                }
            }

            if (sum >= amount) {
                GammaLongRef fromAccount = from.getRandomAccount();
                fromAccount.openForWrite(tx, pessimisticWrite ? LOCKMODE_EXCLUSIVE : LOCKMODE_NONE).long_value -= amount;

                GammaLongRef toAccount = to.getRandomAccount();
                toAccount.openForWrite(tx, pessimisticWrite ? LOCKMODE_EXCLUSIVE : LOCKMODE_NONE).long_value += amount;
            }

            sleepRandomUs(20);
        }
    }

    public User random(User user1, User user2) {
        return randomBoolean() ? user1 : user2;
    }

    public class User {
        private AtomicBlock getTotalBlock = stm.newTransactionFactoryBuilder()
                .setReadonly(true)
                .newAtomicBlock();

        private GammaLongRef account1 = new GammaLongRef(stm);
        private GammaLongRef account2 = new GammaLongRef(stm);

        public GammaLongRef getRandomAccount() {
            return randomBoolean() ? account1 : account2;
        }

        public long getTotal() {
            return getTotalBlock.execute(new AtomicLongClosure() {
                @Override
                public long execute(Transaction tx) throws Exception {
                    GammaTransaction btx = (GammaTransaction) tx;
                    return account1.get(btx) + account2.get(btx);
                }
            });
        }

        public String toString() {
            return stm.newTransactionFactoryBuilder().newAtomicBlock().execute(new AtomicClosure<String>() {
                @Override
                public String execute(Transaction tx) throws Exception {
                    GammaTransaction btx = (GammaTransaction) tx;

                    return format("User(account1 = %s, account2 = %s)",
                            account1.get(btx), account2.get(btx));
                }
            });
        }
    }
}
