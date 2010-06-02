package org.multiverse.integrationtests.financial;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * The MoneyTransferStressTest is a test where money is transferred from one account to another. At random transfers are
 * executed with random amounts between random accounts.
 * <p/>
 * When the test has completed, the total amount of money should be the same as when the test started.
 *
 * @author Peter Veentjer.
 */
public class MoneyTransferStressTest {

    private volatile boolean stop;

    private BankAccount[] accounts;

    @Before
    public void setUp() {
        stop = false;
        clearThreadLocalTransaction();
    }

    @Test
    public void test_10_2() {
        test(10, 2);
    }

    @Test
    public void test_100_10() {
        test(100, 10);
    }

    @Test
    public void test_1000_10() {
        test(1000, 10);
    }

    @Test
    public void test_30_30() {
        test(30, 30);
    }

    public void test(int accountCount, int threadCount) {
        accounts = new BankAccount[accountCount];

        long initialAmount = 0;
        for (int k = 0; k < accountCount; k++) {
            long amount = randomInt(1000);
            initialAmount += amount;
            accounts[k] = new BankAccount(amount);
        }

        TransferThread[] threads = createThreads(threadCount);

        startAll(threads);

        sleepMs(getStressTestDurationMs(60 * 1000));
        stop = true;
        joinAll(threads);

        assertEquals(initialAmount, getTotal());
    }

    private long getTotal() {
        long sum = 0;
        for (BankAccount account : accounts) {
            sum += account.getBalance();
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

    private class TransferThread extends TestThread {

        public TransferThread(int id) {
            super("TransferThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            int k = 0;
            while (!stop) {
                try {
                    transferBetweenRandomAccounts();
                    if ((k % 1000) == 0) {
                        System.out.printf("Thread %s is at iteration %s\n", getName(), k);
                    }
                    k++;
                } catch (NotEnoughMoneyException ignore) {
                }
            }
        }

        @TransactionalMethod
        private void transferBetweenRandomAccounts() {
            BankAccount from = accounts[randomInt(accounts.length)];
            BankAccount to = accounts[randomInt(accounts.length)];
            int amount = randomInt(100);
            to.inc(amount);
            //place some delay so that the transaction is very likely to conflict 
            sleepRandomMs(20);
            from.dec(amount);
        }
    }

    @TransactionalObject
    private static class BankAccount {

        private long balance;

        private BankAccount(final long balance) {
            this.balance = balance;
        }

        @TransactionalMethod(readonly = true)
        public long getBalance() {
            return balance;
        }

        public void setBalance(long balance) {
            if (balance < 0) {
                throw NotEnoughMoneyException.INSTANCE;
            }

            this.balance = balance;
        }

        public void inc(long delta) {
            setBalance(getBalance() + delta);
        }

        public void dec(long delta) {
            setBalance(getBalance() - delta);
        }
    }

    private static class NotEnoughMoneyException extends RuntimeException {
        static NotEnoughMoneyException INSTANCE = new NotEnoughMoneyException();
    }
}
