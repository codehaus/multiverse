package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.stms.alpha.transactions.SpeculativeConfiguration;

import java.util.concurrent.TimeUnit;

/**
 * A performance comparison between the fixed and growing trackingupdatealpha transactions. Gives an indication
 * when one of the two is preferred.
 *
 * @author Peter Veentjer.
 */
public class AbstractTrackingUpdateAlphaTransaction_performanceComparison {

    private AlphaStmConfig stmConfig;
    private AlphaStm stm;
    private SpeculativeConfiguration speculativeConfig;
    private UpdateConfiguration config;
    private ManualRef[] refs;
    //todo: very small size. 
    private int txCount = 1000;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
        speculativeConfig = new SpeculativeConfiguration(100);

        config = new UpdateConfiguration(
                stmConfig.clock,
                stmConfig.backoffPolicy,
                stmConfig.commitLockPolicy,
                null,
                speculativeConfig,
                stmConfig.maxRetryCount, true, true, true, true, true, true, true);

    }

    public AlphaTransaction startFixedTransaction(int size) {
        return new ArrayUpdateAlphaTransaction(config, size);
    }

    public AlphaTransaction startGrowingTransaction() {
        return new MapUpdateAlphaTransaction(config);
    }

    @Test
    public void test_1() {
        test(1);
    }

    @Test
    public void test_2() {
        test(2);
        testWithReuse(2);
    }

    @Test
    public void test_3() {
        test(3);
    }

    @Test
    public void test_4() {
        test(4);
    }

    @Test
    public void test_5() {
        test(5);
    }

    @Test
    public void test_6() {
        test(6);
    }

    @Test
    public void test_7() {
        test(7);
    }

    @Test
    public void test_8() {
        test(8);
    }

    @Test
    public void test_9() {
        test(9);
    }

    @Test
    public void test_10() {
        test(10);
    }

    @Test
    public void test_12() {
        test(12);
    }

    @Test
    public void test_14() {
        test(14);
    }

    @Test
    public void test_17() {
        test(17);
    }

    @Test
    public void test_20() {
        test(20);
    }

    @Test
    public void test_25() {
        test(25);
    }

    @Test
    public void test_30() {
        test(30);
    }

    @Test
    public void test_40() {
        test(40);
    }

    @Test
    public void test_50() {
        test(50);
    }

    @Test
    public void test_75() {
        test(75);
    }

    @Test
    public void test_100() {
        test(100);
    }

    public void test(int transactionSize) {
        refs = new ManualRef[transactionSize];
        for (int k = 0; k < transactionSize; k++) {
            refs[k] = new ManualRef(stm);
        }

        speculativeConfig.setOptimalSize(transactionSize);
        long startFixedNs = System.nanoTime();

        for (int k = 0; k < txCount; k++) {
            AlphaTransaction tx = startFixedTransaction(transactionSize);
            for (int l = 0; l < transactionSize; l++) {
                refs[l].inc(tx);
            }
            tx.commit();
        }

        long periodFixedNs = System.nanoTime() - startFixedNs;

        double fixedTransactionPerSecond = (txCount * TimeUnit.SECONDS.toNanos(1)) / periodFixedNs;

        long startGrowingNs = System.nanoTime();

        for (int k = 0; k < txCount; k++) {
            AlphaTransaction tx = startGrowingTransaction();
            for (int l = 0; l < transactionSize; l++) {
                refs[l].inc(tx);
            }
            tx.commit();
        }

        long periodGrowingNs = System.nanoTime() - startGrowingNs;

        double growingTransactionPerSecond = (txCount * TimeUnit.SECONDS.toNanos(1)) / periodGrowingNs;

        System.out.println("transactionsize: " + transactionSize);
        System.out.printf("growing %s tx/sec\n", growingTransactionPerSecond);
        System.out.printf("fixed %s tx/sec\n", fixedTransactionPerSecond);
        System.out.printf("fixed is %s faster than growing\n", fixedTransactionPerSecond / growingTransactionPerSecond);
    }

    public void testWithReuse(int transactionSize) {
        refs = new ManualRef[transactionSize];
        for (int k = 0; k < transactionSize; k++) {
            refs[k] = new ManualRef(stm);
        }

        speculativeConfig.setOptimalSize(transactionSize);
        long startFixedNs = System.nanoTime();

        AlphaTransaction tx = startFixedTransaction(transactionSize);
        for (int k = 0; k < txCount; k++) {
            for (int l = 0; l < transactionSize; l++) {
                refs[l].inc(tx);
            }
            tx.commit();
            tx.restart();
        }

        long periodFixedNs = System.nanoTime() - startFixedNs;

        double fixedTransactionPerSecond = (txCount * TimeUnit.SECONDS.toNanos(1)) / periodFixedNs;

        long startGrowingNs = System.nanoTime();

        tx = startGrowingTransaction();
        for (int k = 0; k < txCount; k++) {
            for (int l = 0; l < transactionSize; l++) {
                refs[l].inc(tx);
            }
            tx.commit();
            tx.restart();
        }

        long periodGrowingNs = System.nanoTime() - startGrowingNs;

        double growingTransactionPerSecond = (txCount * TimeUnit.SECONDS.toNanos(1)) / periodGrowingNs;

        System.out.println("transactionsize: " + transactionSize);
        System.out.printf("growing %s tx/sec\n", growingTransactionPerSecond);
        System.out.printf("fixed %s tx/sec\n", fixedTransactionPerSecond);
        System.out.printf("fixed is %s faster than growing\n", fixedTransactionPerSecond / growingTransactionPerSecond);
    }


}
