package org.multiverse.stms.alpha.transactions.readonly;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import java.util.concurrent.TimeUnit;

import static org.multiverse.TestUtils.format;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * todo:
 * variable number of threads.
 */
public class MonoReadonlyAlphaTransaction_performanceTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;
    private int transactionCount = 200 * 1000 * 1000;
    private ReadonlyConfiguration config;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stmConfig = AlphaStmConfig.createFastConfig();
        stmConfig.maxRetries = 10;
        stm = new AlphaStm(stmConfig);
        config = new ReadonlyConfiguration(stmConfig.clock, true);
    }

    public MonoReadonlyAlphaTransaction startSutTransaction() {
        return new MonoReadonlyAlphaTransaction(config);
    }

    @Test
    public void whenTransactionsNotReused() {
        ManualRef ref = new ManualRef(stm, 10);

        long startNs = System.nanoTime();

        for (int k = 0; k < transactionCount; k++) {
            AlphaTransaction tx = startSutTransaction();
            tx.openForRead(ref);
            tx.commit();

            if (k % (10 * 1000 * 1000) == 0) {
                System.out.printf("at %s\n", k);
            }
        }

        long periodNs = System.nanoTime() - startNs;
        double transactionPerSecond = (transactionCount * TimeUnit.SECONDS.toNanos(1)) / periodNs;
        System.out.printf("%s read-transactions/second\n", format(transactionPerSecond));
    }

    @Test
    public void whenTransactionReused() {
        ManualRef ref = new ManualRef(stm, 10);

        long startNs = System.nanoTime();

        AlphaTransaction tx = startSutTransaction();

        for (int k = 0; k < transactionCount; k++) {
            tx.restart();
            tx.openForRead(ref);
            tx.commit();

            if (k % (10 * 1000 * 1000) == 0) {
                System.out.printf("at %s\n", k);
            }
        }

        long periodNs = System.nanoTime() - startNs;
        double transactionPerSecond = (transactionCount * TimeUnit.SECONDS.toNanos(1)) / periodNs;
        System.out.printf("%s reads-transactions/second\n", format(transactionPerSecond));
    }
}
