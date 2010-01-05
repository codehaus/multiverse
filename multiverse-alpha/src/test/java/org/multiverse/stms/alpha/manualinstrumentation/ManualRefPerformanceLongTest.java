package org.multiverse.stms.alpha.manualinstrumentation;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.TransactionFactory;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

/**
 * A Performance test that checks various different approaches (from fastest to slowest):
 * <ol>
 * <li>doing operations on the tranlocal directly using a single transaction</li>
 * <li>doing operations using single transaction</li>
 * <li>doing operations using transaction/operation using no transaction management (so no template object)</li>
 * <li>doing operations using transaction/operation using templateobject for transactionmanagement</li>
 * </ol>
 *
 * @author Peter Veentjer.
 */
public class ManualRefPerformanceLongTest {
    private AlphaStm stm;
    private int transactionCount = 40 * 1000 * 1000;
    private TransactionFactory<AlphaTransaction> txFactory;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        txFactory = stm.getTransactionFactoryBuilder()
                .setFamilyName("sometransaction")
                .setReadonly(false)
                .setAutomaticReadTracking(true)
                .build();
    }

    /**
     * Test to see what the overhead of just creating a transaction object is.
     */
    @Test
    public void transactionCreationTest(){
        int realCount = 15 * transactionCount;

        long startNs = System.nanoTime();

        for (int k = 0; k < realCount; k++) {
            txFactory.start();

            if (k % 50000000 == 0) {
                System.out.println("at " + k);
            }
        }

        long periodNs = System.nanoTime() - startNs;
        double transactionPerSecond = (realCount * TimeUnit.SECONDS.toNanos(1)) / periodNs;
        System.out.printf("%s transaction/second\n", transactionPerSecond);
    }

    @Test
    public void testAtomicInteger(){
        AtomicInteger ref = new AtomicInteger();

        int realCount = 10 * transactionCount;

        long startNs = System.nanoTime();

        for (int k = 0; k < realCount; k++) {
            ref.incrementAndGet();

            if (k % 10000000 == 0) {
                System.out.println("at " + k);
            }
        }

        long periodNs = System.nanoTime() - startNs;
        double transactionPerSecond = (realCount * TimeUnit.SECONDS.toNanos(1)) / periodNs;
        System.out.println("---- atomicInteger --------------------------");
        System.out.printf("%s inc/second\n", transactionPerSecond);
        System.out.println("---------------------------------------------");
    }

    @Test
    public void testSingleTransactionAndWorkingOnTranlocalDirectly() {
        ManualRef ref = new ManualRef(stm);

        int realCount = 100 * transactionCount;

        long startNs = System.nanoTime();

        AlphaTransaction tx = txFactory.start();
        ManualRefTranlocal tranlocal = (ManualRefTranlocal) tx.openForWrite(ref);
        for (int k = 0; k < realCount; k++) {
            tranlocal.value++;

            if (k % 100000000 == 0) {
                System.out.println("at " + k);
            }
        }
        tx.commit();

        long periodNs = System.nanoTime() - startNs;
        double transactionPerSecond = (realCount * TimeUnit.SECONDS.toNanos(1)) / periodNs;
        System.out.printf("%s inc/second\n", transactionPerSecond);
    }


    @Test
    public void testSingleTransaction() {
        ManualRef ref = new ManualRef(stm);

        int realCount = 100 * transactionCount;

        long startNs = System.nanoTime();

        AlphaTransaction tx = txFactory.start();
        for (int k = 0; k < realCount; k++) {
            ref.inc(tx);

            if (k % 50000000 == 0) {
                System.out.println("at " + k);
            }
        }
        tx.commit();

        long periodNs = System.nanoTime() - startNs;
        double transactionPerSecond = (realCount * TimeUnit.SECONDS.toNanos(1)) / periodNs;
        System.out.printf("%s inc/second\n", transactionPerSecond);
    }

    @Test
    public void testMultipleTransactionAndNoTransactionManagement() {
        ManualRef ref = new ManualRef(stm);

        long startNs = System.nanoTime();


        for (int k = 0; k < transactionCount; k++) {
            AlphaTransaction tx = txFactory.start();
            ref.inc(tx);
            tx.commit();

            if (k % 1000000 == 0) {
                System.out.println("at " + k);
            }
        }

        long periodNs = System.nanoTime() - startNs;
        double transactionPerSecond = (transactionCount * TimeUnit.SECONDS.toNanos(1)) / periodNs;
        System.out.printf("%s inc/second\n", transactionPerSecond);
    }

    @Test
    public void testMultipleTransaction() {
        ManualRef ref = new ManualRef(stm);

        long startNs = System.nanoTime();

         for (int k = 0; k < transactionCount; k++) {
            ref.inc(txFactory);

            if (k % 1000000 == 0) {
                System.out.println("at " + k);
            }
        }

        long periodNs = System.nanoTime() - startNs;
        double transactionPerSecond = (transactionCount * TimeUnit.SECONDS.toNanos(1)) / periodNs;
        System.out.printf("%s inc/second\n", transactionPerSecond);
    }

    @Test
    public void testMultipleTransactionWithFastRef() {
        FastManualRef ref = new FastManualRef(stm);

        long startNs = System.nanoTime();

        for (int k = 0; k < transactionCount; k++) {
            ref.fastIncWithThreadLocal(txFactory);

            if (k % 1000000 == 0) {
                System.out.println("at " + k);
            }
        }

        long periodNs = System.nanoTime() - startNs;
        double transactionPerSecond = (transactionCount * TimeUnit.SECONDS.toNanos(1)) / periodNs;
        System.out.printf("%s inc/second\n", transactionPerSecond);
    }

    @Test
    public void testMultipleTransactionWithFastRefAndNoThreadLocalUsage() {
        FastManualRef ref = new FastManualRef(stm);

        long startNs = System.nanoTime();

        for (int k = 0; k < transactionCount; k++) {
            ref.fastIncWithoutThreadLocal(txFactory);

            if (k % 1000000 == 0) {
                System.out.println("at " + k);
            }
        }

        long periodNs = System.nanoTime() - startNs;
        double transactionPerSecond = (transactionCount * TimeUnit.SECONDS.toNanos(1)) / periodNs;
        System.out.printf("%s inc/second\n", transactionPerSecond);
    }
}
