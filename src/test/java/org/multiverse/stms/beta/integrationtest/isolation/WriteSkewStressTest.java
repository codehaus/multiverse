package org.multiverse.stms.beta.integrationtest.isolation;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.AtomicBlock;
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
    private boolean allowWriteSkew;
    private TransferThread[] threads;
    private AtomicBoolean writeSkewEncountered = new AtomicBoolean();
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
        clearThreadLocalTransaction();
        user1 = new User();
        user2 = new User();
        stop = false;
        writeSkewEncountered.set(false);

        threads = new TransferThread[2];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new TransferThread(k);
        }

        IntRef account = user1.getRandomAccount();
        BetaTransaction tx = stm.startDefaultTransaction();
        tx.openForWrite(account, false, pool).value = 1000;
        tx.commit();
    }

    @Test
    @Ignore
    public void whenPessimisticLockingUsed(){

    }

    @Test
    @Ignore
    public void whenLockedRead(){

    }

    @Test
    public void whenWriteSkewAllowed() {
        allowWriteSkew = true;
        startAll(threads);
        sleepMs(getStressTestDurationMs(60 * 1000));
        stop = true;
        joinAll(threads);

        System.out.println("User1: " + user1);
        System.out.println("User2: " + user2);

        assertTrue(writeSkewEncountered.get());
    }

    @Test
    public void whenWriteSkewNotAllowed() {
        allowWriteSkew = false;
        startAll(threads);
        sleepMs(getStressTestDurationMs(60 * 1000));
        stop = true;

        joinAll(threads);

        System.out.println("User1: " + user1);
        System.out.println("User2: " + user2);

        assertFalse("writeskew detected", writeSkewEncountered.get());
    }

    public class TransferThread extends TestThread {

        private final AtomicBlock withWriteSkew = stm.getTransactionFactoryBuilder()
                .setWriteSkewAllowed(true)
                .buildAtomicBlock();

        private final AtomicBlock noWriteSkew = stm.getTransactionFactoryBuilder()
                .setWriteSkewAllowed(false)
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

                if (allowWriteSkew) {
                    doItWithWriteSkew();
                } else {
                    doItWithNoWriteSkew();
                }
                k++;
            }
        }

        private void doItWithNoWriteSkew() {
            noWriteSkew.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    BetaObjectPool pool = getThreadLocalBetaObjectPool();
                    doIt(btx, pool);
                }
            });
        }

        private void doItWithWriteSkew() {
            withWriteSkew.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    BetaObjectPool pool = getThreadLocalBetaObjectPool();
                    doIt(btx, pool);
                }
            });
        }

        public void doIt(BetaTransaction tx, BetaObjectPool pool) {
            int amount = randomInt(100);

            User from = random(user1, user2);
            User to = random(user1, user2);

            int sum = from.account1.get(tx, pool) + from.account2.get(tx, pool);

            if (sum < 0) {
                if (!writeSkewEncountered.get()) {
                    writeSkewEncountered.set(true);
                    System.out.println("writeskew detected");
                }
            }

            if (sum >= amount) {
                IntRef fromAccount = from.getRandomAccount();                                
                fromAccount.set(tx, pool, fromAccount.get(tx, pool) - amount);

                IntRef toAccount = to.getRandomAccount();
                toAccount.set(tx, pool, toAccount.get(tx, pool) + amount);
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
