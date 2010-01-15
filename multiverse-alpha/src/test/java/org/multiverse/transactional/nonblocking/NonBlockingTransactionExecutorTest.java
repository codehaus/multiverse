package org.multiverse.transactional.nonblocking;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.transactional.primitives.TransactionalInteger;

import static org.multiverse.TestUtils.randomInt;
import static org.multiverse.TestUtils.sleepRandomMs;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

public class NonBlockingTransactionExecutorTest {

    private NonBlockingTransactionExecutor nonBlockingTransactionExecutor;
    private TransactionalInteger[] refs;
    private Stm stm;
    private TransactionFactory transactionFactory;
    private int refCount = 10000;

    @Before
    public void setUp() {
        nonBlockingTransactionExecutor = new NonBlockingTransactionExecutor(3);
        nonBlockingTransactionExecutor.start();

        stm = getGlobalStmInstance();
        refs = new TransactionalInteger[refCount];
        for (int k = 0; k < refCount; k++) {
            refs[k] = new TransactionalInteger();
        }
        transactionFactory = stm.getTransactionFactoryBuilder()
                .setReadonly(false)
                .setAutomaticReadTracking(true)
                .build();
    }

    @Test
    @Ignore
    public void test() {
        for (int k = 0; k < refCount; k++) {
            nonBlockingTransactionExecutor.execute(new DecTask(transactionFactory, k));
        }

        for (int k = 0; k < 1000; k++) {
            sleepRandomMs(1000);
            int randomIndex = randomInt(refCount);
            refs[randomIndex].inc();
        }
    }

    public class DecTask implements TransactionalTask {

        private final TransactionFactory transactionFactory;
        private int refIndex;
        private int count;

        public DecTask(TransactionFactory transactionFactory, int refIndex) {
            this.transactionFactory = transactionFactory;
            this.refIndex = refIndex;
        }

        @Override
        public TransactionFactory getTransactionFactory() {
            return transactionFactory;
        }

        @Override
        public boolean execute(Transaction t) {
            TransactionalInteger ref = refs[refIndex];
            ref.await(1);
            count++;
            System.out.println("End wait ref: " + refIndex + " count: " + count);
            ref.dec();
            return count != 10;
        }
    }
}
