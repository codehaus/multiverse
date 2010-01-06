package org.multiverse.stms.alpha.manualinstrumentation;

import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import org.multiverse.api.Stm;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import org.multiverse.api.Transaction;
import org.multiverse.stms.alpha.AlphaStm;

import java.util.concurrent.TimeUnit;

public class IntStackPerformanceLongTest {

    private int count = 5 * 1000 * 1000;

    private Stm stm;

    @Before
    public void setUp() {
        stm = AlphaStm.createFast();
        setGlobalStmInstance(stm);
        setThreadLocalTransaction(null);
    }


    public Transaction startTransaction() {
        Transaction t = stm.startUpdateTransaction(null);
        setThreadLocalTransaction(t);
        return t;
    }

    @Test
    public void test() {
        Transaction t = startTransaction();
        IntStack stack = new IntStack();
        t.commit();

        long startNs = System.nanoTime();

        for (int k = 0; k < count; k++) {
            Transaction t2 = startTransaction();
            stack.push(10);
            t2.commit();

            Transaction t3 = startTransaction();
            stack.pop();
            t3.commit();
        }

        long periodNs = System.nanoTime() - startNs;
        double transactionPerSecond = (count * 2.0d * TimeUnit.SECONDS.toNanos(1)) / periodNs;
        System.out.printf("%s Transaction/second\n", transactionPerSecond);
    }

    @Test
    public void testOptimizedTransactionRetrieval() {
        Transaction t = startTransaction();
        IntStack stack = new IntStack();
        t.commit();

        long startNs = System.nanoTime();

        for (int k = 0; k < count; k++) {
            Transaction t2 = startTransaction();
            stack.push(t2, 10);
            t2.commit();

            Transaction t3 = startTransaction();
            stack.pop(t3);
            t3.commit();
        }

        long periodNs = System.nanoTime() - startNs;
        double transactionPerSecond = (count * 2.0d * TimeUnit.SECONDS.toNanos(1)) / periodNs;
        System.out.printf("%s Transaction/second\n", transactionPerSecond);
    }
}
