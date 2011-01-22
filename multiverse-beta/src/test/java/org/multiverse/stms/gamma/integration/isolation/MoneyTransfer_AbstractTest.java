package org.multiverse.stms.gamma.integration.isolation;

import org.junit.Before;
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
public abstract class MoneyTransfer_AbstractTest {

    private volatile boolean stop;

    private GammaLongRef[] accounts;
    protected GammaStm stm;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stop = false;
        stm = (GammaStm) getGlobalStmInstance();
    }

    protected abstract AtomicBlock newAtomicBlock();

    public void run(int accountCount, int threadCount) {
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
            AtomicBlock block = newAtomicBlock();

            AtomicVoidClosure closure = new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    GammaTransaction btx = (GammaTransaction) tx;
                    GammaLongRef from = accounts[randomInt(accounts.length)];
                    GammaLongRef to = accounts[randomInt(accounts.length)];
                    int amount = randomInt(100);

                    to.openForWrite(btx, LOCKMODE_NONE).long_value += amount;

                    sleepRandomMs(10);

                    GammaRefTranlocal toTranlocal = from.openForWrite(btx, LOCKMODE_NONE);
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
