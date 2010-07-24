package org.multiverse.stms.beta.integrationtest;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.ObjectPool;
import org.multiverse.stms.beta.TransactionTemplate;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.transactions.ArrayBetaTransaction;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertEquals;
import static org.multiverse.stms.beta.StmUtils.createLongRef;
import static org.multiverse.TestUtils.randomInt;
import static org.multiverse.TestUtils.sleepMs;
import static org.multiverse.TestUtils.sleepRandomMs;

public class MoneyTransferStressTest {

    private volatile boolean stop;

    private LongRef[] accounts;
    private BetaStm stm;

    @Before
    public void setUp() {
        stop = false;
        stm = new BetaStm();
    }

    @Test
    public void test_10Accounts_2Threads() throws InterruptedException {
        test(10, 2);
    }

    @Test
    public void test_100Account_10Threads() throws InterruptedException {
        test(100, 10);
    }

    @Test
    public void test_1000Accounts_10Threads() throws InterruptedException {
        test(1000, 10);
    }

    @Test
    public void test_30Accounts_30Threads() throws InterruptedException {
        test(30, 30);
    }

    public void test(int accountCount, int threadCount) throws InterruptedException {
        accounts = new LongRef[accountCount];

        long initialAmount = 0;
        for (int k = 0; k < accountCount; k++) {
            long amount = randomInt(1000);
            initialAmount += amount;
            accounts[k] = createLongRef(stm, amount);
        }

        TransferThread[] threads = createThreads(threadCount);

        for (TransferThread thread : threads) {
            thread.start();
        }

        sleepMs(30 * 1000);


        stop = true;
        for (TransferThread thread : threads) {
            thread.join();
        }


        assertEquals(initialAmount, getTotal());
    }

    private long getTotal() {
        long sum = 0;
        for (LongRef account : accounts) {
            sum += account.unsafeLoad().value;
        }
        return sum;
    }

    private TransferThread[] createThreads(int threadCount) {
        TransferThread[] threads = new TransferThread[threadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new TransferThread(k);
        }
        return threads;
    }

    private class TransferThread extends Thread {

        private final ObjectPool pool = new ObjectPool();
        private TransactionTemplate transactionTemplate;

        public TransferThread(int id) {
            super("TransferThread-" + id);

            transactionTemplate = new TransactionTemplate(stm, new ArrayBetaTransaction(stm,10)) {
                @Override
                public void execute(BetaTransaction tx) {
                    LongRef from = accounts[randomInt(accounts.length)];
                    LongRef to = accounts[randomInt(accounts.length)];
                    int amount = randomInt(100);

                    tx.openForWrite(to, false, pool).value+=amount;

                    sleepRandomMs(20);

                    tx.openForWrite(from, false, pool).value-=amount;

                    //place some delay so that the transaction is very likely to conflict

                }
            };
        }

        public void run() {
            int k = 0;

            while (!stop) {
                try {
                    transactionTemplate.execute(pool);
                    if ((k % 1000) == 0) {
                        System.out.printf("Thread %s is at iteration %s\n", getName(), k);
                    }
                    k++;
                } catch (NotEnoughMoneyException ignore) {
                }
            }
        }
    }


    private static class NotEnoughMoneyException extends RuntimeException {
        static NotEnoughMoneyException INSTANCE = new NotEnoughMoneyException();
    }

}
