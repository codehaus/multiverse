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

    private final int threadCount = 20;
    private final int accountCount = 10;
    private final int transferCount = 1000;

    private long initialAmount;
    private BankAccount[] bankAccounts;
    private TransferThread[] threads;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();

        bankAccounts = new BankAccount[accountCount];

        for (int k = 0; k < accountCount; k++) {
            long amount = randomInt(1000);
            initialAmount += amount;
            bankAccounts[k] = new BankAccount(amount);
        }

        threads = createThreads();
    }


    @Test
    public void test() {
        startAll(threads);
        joinAll(threads);

        assertEquals(initialAmount, getTotal());
    }

    private long getTotal() {
        long sum = 0;
        for (BankAccount account : bankAccounts) {
            sum += account.getBalance();
        }
        return sum;
    }

    private TransferThread[] createThreads() {
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
            do {
                try {
                    transferBetweenRandomAccounts();
                    if ((k % 100) == 0) {
                        System.out.printf("Thread %s is at iteration %s\n", getName(), k);
                    }
                    k++;
                } catch (NotEnoughMoneyException ignore) {
                }
            } while (k < transferCount);
        }

        @TransactionalMethod
        private void transferBetweenRandomAccounts() {
            BankAccount from = bankAccounts[randomInt(bankAccounts.length - 1)];
            BankAccount to = bankAccounts[randomInt(bankAccounts.length - 1)];
            int amount = randomInt(1000);
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
                throw new NotEnoughMoneyException();
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

    }
}
