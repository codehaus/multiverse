package org.multiverse.stms.gamma.benchmarks;

import org.benchy.BenchyUtils;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactions.GammaTransactionConfiguration;
import org.multiverse.stms.gamma.transactions.MonoGammaTransaction;

import static org.junit.Assert.assertEquals;

public class MonoUpdateDriver implements GammaConstants {

    private GammaStm stm;

    @Before
    public void setUp(){
         stm = new GammaStm();
    }

    public static void main(String[] srgs){
        MonoUpdateDriver driver = new MonoUpdateDriver();
        driver.setUp();
        driver.test();
    }

    @Test
    public void test(){
        final long txCount = 1000 * 1000 * 1000;

        MonoGammaTransaction tx = new MonoGammaTransaction(new GammaTransactionConfiguration(stm));
        GammaLongRef ref = new GammaLongRef(stm, 0);
        long initialVersion = ref.getVersion();

        long startMs = System.currentTimeMillis();
        for(long k=0;k<txCount;k++){
            ref.openForWrite(tx,LOCKMODE_NONE).long_value++;
            tx.commit();
            tx.hardReset();
        }
        long durationMs = System.currentTimeMillis()-startMs;

        String s = BenchyUtils.operationsPerSecondPerThreadAsString(txCount, durationMs,1);

        System.out.printf("Performance is %s transactions/second/thread\n", s);

        assertEquals(txCount, ref.value);
        assertEquals(txCount+initialVersion, ref.version);

    }
}
