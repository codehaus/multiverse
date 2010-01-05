package org.multiverse.stms.alpha.transactions.readonly;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.stms.alpha.transactions.OptimalSize;

import java.util.concurrent.TimeUnit;

public class TinyReadonlyAlphaTransaction_performanceLongTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;
    private OptimalSize optimalSize;
    private int transactionCount = 200 * 1000 * 1000;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createFastConfig();
        stm = new AlphaStm(stmConfig);
        optimalSize = new OptimalSize(1);
    }

    public TinyReadonlyAlphaTransaction startSutTransaction() {
        TinyReadonlyAlphaTransaction.Config config = new TinyReadonlyAlphaTransaction.Config(
                stmConfig.clock,
                stmConfig.restartBackoffPolicy,
                null,
                stmConfig.profiler,
                stmConfig.maxRetryCount, true, optimalSize);
        return new TinyReadonlyAlphaTransaction(config);
    }

    @Test
    public void testNoTxManagement(){
        ManualRef ref = new ManualRef(stm, 10);

        long startNs = System.nanoTime();

        for(int k=0;k<transactionCount;k++){
            AlphaTransaction tx = startSutTransaction();
            tx.openForRead(ref);
            tx.commit();

            if(k % (10*1000*1000)==0){
                System.out.printf("at %s\n",k);
            }
        }

        long periodNs = System.nanoTime() - startNs;
        double transactionPerSecond = (transactionCount * TimeUnit.SECONDS.toNanos(1)) / periodNs;
        System.out.println("---- no tx management -----------------------");
        System.out.printf("%s reads/second\n", transactionPerSecond);
        System.out.println("---------------------------------------------");
    }

    @Test
    public void testNoTxManagementAndTxReuse(){
        ManualRef ref = new ManualRef(stm, 10);

        long startNs = System.nanoTime();

        AlphaTransaction tx = startSutTransaction();

        for(int k=0;k<transactionCount;k++){
            tx.restart();
            tx.openForRead(ref);
            tx.commit();

            if(k % (10*1000*1000)==0){
                System.out.printf("at %s\n",k);
            }
        }

        long periodNs = System.nanoTime() - startNs;
        double transactionPerSecond = (transactionCount * TimeUnit.SECONDS.toNanos(1)) / periodNs;
        System.out.println("---- no tx management + tx reuse ------------");
        System.out.printf("%s reads/second\n", transactionPerSecond);
        System.out.println("---------------------------------------------");
    }
}
