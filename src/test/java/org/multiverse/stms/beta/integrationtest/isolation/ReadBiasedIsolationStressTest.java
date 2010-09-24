package org.multiverse.stms.beta.integrationtest.isolation;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicBooleanClosure;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.orec.FastOrec;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.randomOneOf;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
import static org.multiverse.stms.beta.benchmarks.BenchmarkUtils.transactionsPerSecondAsString;
import static org.multiverse.stms.beta.benchmarks.BenchmarkUtils.transactionsPerSecondPerThreadAsString;

/**
 * A Stress test that checks if the system is able to deal with mostly reading transactions and doesn't cause
 * any isolation problems.
 *
 * @author Peter Veentjer.
 */
public class ReadBiasedIsolationStressTest {

    private BetaStm stm;
    private int chanceOfUpdate = new FastOrec().___getReadBiasedThreshold() * 5;
    private int threadCount = 4;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = new BetaStm();
    }

    @Test
    public void withOptimisticSettingAndDirtyCheck() {
        test(false, true);
    }

    @Test
    public void withPessimisticSettingsAndDirtyCheck() {
        test(true, true);
    }

    @Test
    public void withOptimisticSettingAndNoDirtyCheck() {
        test(false, false);
    }

    @Test
    public void withPessimisticSettingsAndNoDirtyCheck() {
        test(true, false);
    }

    public void test(boolean pessimistic, boolean dirtyCheckEnabled) {
        StressThread[] threads = new StressThread[threadCount];
        BetaLongRef ref = newLongRef(stm);
        long transactionsPerThread = 100 * 1000 * 1000;

        for (int k = 0; k < threads.length; k++) {
            threads[k] = new StressThread(k, ref, transactionsPerThread, pessimistic, dirtyCheckEnabled);
        }

        for (StressThread thread : threads) {
            thread.start();
        }

        joinAll(threads);

        long totalDurationMs = 0;
        long sum = 0;
        for (StressThread thread : threads) {
            totalDurationMs += thread.durationMs;
            sum += thread.incrementCount;
        }

        System.out.println("--------------------------------------------------------");
        System.out.printf("Threadcount:       %s\n", threadCount);
        System.out.printf("Performance:       %s transactions/second/thread\n",
                transactionsPerSecondPerThreadAsString(transactionsPerThread, totalDurationMs, threadCount));
        System.out.printf("Performance:       %s transactions/second\n",
                transactionsPerSecondAsString(transactionsPerThread, totalDurationMs, threadCount));

        assertEquals(sum, ref.atomicGet());
        System.out.println("ref.orec: " + ref.___toOrecString());
    }

    class StressThread extends TestThread {
        private final boolean dirtyCheckEnabled;
        private final BetaLongRef ref;
        private final long count;
        private long durationMs;
        private boolean pessimistic;
        private long incrementCount = 0;

        public StressThread(int id, BetaLongRef ref, long count, boolean pessimistic, boolean dirtyCheckEnabled) {
            super("StressThread-" + id);
            this.ref = ref;
            this.count = count;
            this.pessimistic = pessimistic;
            this.dirtyCheckEnabled = dirtyCheckEnabled;
        }

        @Override
        public void doRun() {
            AtomicBlock block = stm.createTransactionFactoryBuilder()
                    .setDirtyCheckEnabled(dirtyCheckEnabled)
                    .buildAtomicBlock();

            AtomicBooleanClosure closure = new AtomicBooleanClosure() {
                @Override
                public boolean execute(Transaction tx) throws Exception {
                    if (pessimistic) {
                        ref.ensure(tx);
                    }

                    if (randomOneOf(chanceOfUpdate)) {
                        ref.incrementAndGet(tx, 1);
                        return true;
                    } else {
                        ref.get(tx);
                        return false;
                    }
                }
            };

            long startMs = currentTimeMillis();

            for (long k = 0; k < count; k++) {
                if (block.execute(closure)) {
                    incrementCount++;
                }

                if (k % 10000000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }

            durationMs = currentTimeMillis() - startMs;

            System.out.printf("finished %s after %s ms\n", getName(), durationMs);
        }
    }
}
