package org.multiverse.stms.alpha.manualinstrumentation;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.TransactionFactory;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import java.util.concurrent.TimeUnit;

import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class IntStackPerformanceStressTest {

    private int count = 50 * 1000 * 1000;

    private AlphaStm stm;
    private TransactionFactory txFactory;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        setThreadLocalTransaction(null);
        txFactory = stm.getTransactionFactoryBuilder()
                .setReadonly(false)
                .setAutomaticReadTracking(false)
                .build();
    }


    public AlphaTransaction startTransaction() {
        AlphaTransaction t = (AlphaTransaction) txFactory.start();
        setThreadLocalTransaction(t);
        return t;
    }

    @Test
    public void test() {
        IntStack stack = new IntStack();

        long startNs = System.nanoTime();

        for (int k = 0; k < count; k++) {
            stack.push(10);
            stack.pop();

            if (k % (500 * 1000) == 0) {
                System.out.printf("at %s\n", k);
            }
        }

        long periodNs = System.nanoTime() - startNs;
        double transactionPerSecond = (count * 2.0d * TimeUnit.SECONDS.toNanos(1)) / periodNs;
        System.out.printf("%s Transaction/second\n", transactionPerSecond);
    }

    @Test
    public void testOptimizedTransactionRetrieval() {
        AlphaTransaction t = startTransaction();
        IntStack stack = new IntStack();
        t.commit();

        long startNs = System.nanoTime();

        AlphaTransaction pushTx = startTransaction();
        AlphaTransaction popTx = startTransaction();

        for (int k = 0; k < count; k++) {
            pushTx.restart();
            stack.push(pushTx, 10);
            pushTx.commit();

            popTx.restart();
            stack.pop(popTx);
            popTx.commit();


            if (k % 500000 == 0) {
                System.out.printf("at %s\n", k);
            }
        }

        long periodNs = System.nanoTime() - startNs;
        double transactionPerSecond = (count * 2.0d * TimeUnit.SECONDS.toNanos(1)) / periodNs;
        System.out.printf("%s Transaction/second\n", transactionPerSecond);
    }
}
