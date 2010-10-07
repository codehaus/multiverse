package org.multiverse.stms.beta.integrationtest.isolation;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;

public class MoneyTransferStressTest {

    private volatile boolean stop;

    private BetaLongRef[] accounts;
    private BetaStm stm;
    private boolean optimistic;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stop = false;
        stm = new BetaStm();
    }

    @Test
    public void when10AccountsAnd2ThreadsAndOptimistic() {
        test(10, 2, true);
    }

    @Test
    public void when10AccountsAnd2ThreadsAndPessimistic() {
        test(10, 2, false);
    }

    @Test
    public void when100AccountAnd10ThreadsAndOptimistic() {
        test(100, 10, true);
    }

    @Test
    public void when100AccountAnd10ThreadsAndPessimistic() {
        test(100, 10, false);
    }

    @Test
    public void when1000AccountsAnd10ThreadsAndOptimistic() {
        test(1000, 10, true);
    }

    @Test
    public void when1000AccountsAnd10ThreadsAndPessimistic() {
        test(1000, 10, false);
    }

    @Test
    public void when30AccountsAnd30ThreadsAndOptimistic() {
        test(30, 30, true);
    }

    @Test
    public void when30AccountsAnd30ThreadsAndPessimistic() {
        test(30, 30, false);
    }

    public void test(int accountCount, int threadCount, boolean optimistic) {
        this.optimistic = optimistic;
        accounts = new BetaLongRef[accountCount];

        long initialAmount = 0;
        for (int k = 0; k < accountCount; k++) {
            long amount = randomInt(1000);
            initialAmount += amount;
            accounts[k] = newLongRef(stm, amount);
        }

        TransferThread[] threads = createThreads(threadCount);

        startAll(threads);

        sleepMs(30 * 1000);

        stop = true;

        joinAll(threads);

        assertEquals(initialAmount, getTotal());
    }

    private long getTotal() {
        long sum = 0;
        for (BetaLongRef account : accounts) {
            sum += account.atomicGet();
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

        public void doRun() {
            AtomicBlock block = stm.createTransactionFactoryBuilder().buildAtomicBlock();
            AtomicVoidClosure closure = new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    BetaLongRef from = accounts[randomInt(accounts.length)];
                    BetaLongRef to = accounts[randomInt(accounts.length)];
                    int amount = randomInt(100);

                    btx.openForWrite(to, !optimistic?LOCKMODE_NONE:LOCKMODE_COMMIT).value += amount;

                    sleepRandomMs(10);

                    LongRefTranlocal toTranlocal = btx.openForWrite(from, !optimistic?LOCKMODE_NONE:LOCKMODE_COMMIT);
                    if (toTranlocal.value < 0) {
                        throw new NotEnoughMoneyException();
                    }

                    toTranlocal.value -= amount;
                }
            };

            int k = 0;
            while (!stop) {
                try {
                    block.execute(closure);
                    if ((k % 500) == 0) {
                        System.out.printf("%s is at iteration %s\n", getName(), k);
                    }
                    k++;
                } catch (NotEnoughMoneyException ignore) {
                }
            }
        }
    }

    private static class NotEnoughMoneyException extends RuntimeException {
    }
}
