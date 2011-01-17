package org.multiverse.stms.gamma.integration.isolation;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaRefTranlocal;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

//todo: testing of different lock modes
//todo: testing if multiple transfers are done
public class MoneyTransferStressTest {

    private volatile boolean stop;

    private GammaLongRef[] accounts;
    private GammaStm stm;
    private boolean optimistic;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stop = false;
        stm = (GammaStm) getGlobalStmInstance();
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
        accounts = new GammaLongRef[accountCount];

        long initialAmount = 0;
        for (int k = 0; k < accountCount; k++) {
            long amount = randomInt(1000);
            initialAmount += amount;
            accounts[k] = new GammaLongRef(stm, amount);
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
        for (GammaLongRef account : accounts) {
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
            AtomicBlock block = stm.newTransactionFactoryBuilder().buildAtomicBlock();
            AtomicVoidClosure closure = new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    GammaTransaction btx = (GammaTransaction) tx;
                    GammaLongRef from = accounts[randomInt(accounts.length)];
                    GammaLongRef to = accounts[randomInt(accounts.length)];
                    int amount = randomInt(100);

                    to.openForWrite(btx, !optimistic ? LOCKMODE_NONE : LOCKMODE_EXCLUSIVE).long_value += amount;

                    sleepRandomMs(10);

                    GammaRefTranlocal toTranlocal = from.openForWrite(btx, !optimistic ? LOCKMODE_NONE : LOCKMODE_EXCLUSIVE);
                    if (toTranlocal.long_value < 0) {
                        throw new NotEnoughMoneyException();
                    }

                    toTranlocal.long_value -= amount;
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
