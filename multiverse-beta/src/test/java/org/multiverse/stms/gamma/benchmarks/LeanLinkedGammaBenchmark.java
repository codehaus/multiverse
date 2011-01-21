package org.multiverse.stms.gamma.benchmarks;

import org.benchy.BenchyUtils;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaRef;
import org.multiverse.stms.gamma.transactions.lean.LeanLinkedGammaTransaction;

public class LeanLinkedGammaBenchmark {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    @Test
    public void testRead1() {
        final long txCount = 1000 * 1000 * 1000;
        GammaRef<String> ref1 = new GammaRef<String>(stm);
        LeanLinkedGammaTransaction tx = new LeanLinkedGammaTransaction(stm);

        long startMs = System.currentTimeMillis();

        for (long k = 0; k < txCount; k++) {
            ref1.openForRead(tx);
            tx.commit();
            tx.hardReset();
        }

        long durationMs = System.currentTimeMillis() - startMs;

        String s = BenchyUtils.operationsPerSecondPerThreadAsString(txCount, durationMs, 1);

        System.out.printf("Performance is %s transactions/second/thread\n", s);


        System.out.println(ref1.toDebugString());
    }

    @Test
    public void testRead2() {
        final long txCount = 1000 * 1000 * 1000;
        GammaRef<String> ref1 = new GammaRef<String>(stm);
        GammaRef<String> ref2 = new GammaRef<String>(stm);
        LeanLinkedGammaTransaction tx = new LeanLinkedGammaTransaction(stm);

        long startMs = System.currentTimeMillis();

        for (long k = 0; k < txCount; k++) {
            ref1.openForRead(tx);
            ref2.openForRead(tx);
            tx.commit();
            tx.hardReset();
        }

        long durationMs = System.currentTimeMillis() - startMs;

        String s = BenchyUtils.operationsPerSecondPerThreadAsString(txCount, durationMs, 1);

        System.out.printf("Performance is %s transactions/second/thread\n", s);


        System.out.println(ref1.toDebugString());
    }

    @Test
    public void testRead3() {
        final long txCount = 1000 * 1000 * 1000;
        GammaRef<String> ref1 = new GammaRef<String>(stm);
        GammaRef<String> ref2 = new GammaRef<String>(stm);
        GammaRef<String> ref3 = new GammaRef<String>(stm);
        LeanLinkedGammaTransaction tx = new LeanLinkedGammaTransaction(stm);

        long startMs = System.currentTimeMillis();

        for (long k = 0; k < txCount; k++) {
            ref1.openForRead(tx);
            ref2.openForRead(tx);
            ref3.openForRead(tx);
            tx.commit();
            tx.hardReset();
        }

        long durationMs = System.currentTimeMillis() - startMs;

        String s = BenchyUtils.operationsPerSecondPerThreadAsString(txCount, durationMs, 1);

        System.out.printf("Performance is %s transactions/second/thread\n", s);


        System.out.println(ref1.toDebugString());
    }
}
