package org.multiverse.stms.gamma.benchmarks;

import org.benchy.BenchyUtils;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.LeanGammaAtomicBlock;
import org.multiverse.stms.gamma.transactionalobjects.GammaRef;
import org.multiverse.stms.gamma.transactions.lean.LeanMonoGammaTransaction;
import org.multiverse.stms.gamma.transactions.lean.LeanMonoGammaTransactionFactory;

import static org.junit.Assert.assertEquals;

public class LeanMonoReadWithTransactionDriver implements GammaConstants {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    public static void main(String[] srgs) throws Throwable {
        LeanMonoReadWithTransactionDriver driver = new LeanMonoReadWithTransactionDriver();
        driver.setUp();
        try {
            driver.test();
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void test() {
        final long txCount = 1000 * 1000 * 1000;

        final GammaRef<String> ref = new GammaRef<String>(stm, null);
        long initialVersion = ref.getVersion();

        final LeanGammaAtomicBlock block = new LeanGammaAtomicBlock(new LeanMonoGammaTransactionFactory(stm));

        final AtomicVoidClosure closure = new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                Object x = ref.openForRead((LeanMonoGammaTransaction) tx, LOCKMODE_NONE).ref_value;
            }
        };

        System.out.println("Starting");
        long startMs = System.currentTimeMillis();

        for (long k = 0; k < txCount; k++) {
            block.execute(closure);
        }

        long durationMs = System.currentTimeMillis() - startMs;
        System.out.println("finished");

        String s = BenchyUtils.operationsPerSecondPerThreadAsString(txCount, durationMs, 1);

        System.out.printf("Duration %s ms\n", durationMs);
        System.out.printf("Performance is %s transactions/second/thread\n", s);

        //assertEquals(txCount, ref.long_value);
        assertEquals(initialVersion, ref.version);
    }
}
