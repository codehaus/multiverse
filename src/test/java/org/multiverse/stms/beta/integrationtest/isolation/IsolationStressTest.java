package org.multiverse.stms.beta.integrationtest.isolation;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmUtils;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.benchmarks.BenchmarkUtils.transactionsPerSecondAsString;
import static org.multiverse.stms.beta.benchmarks.BenchmarkUtils.transactionsPerSecondPerThreadAsString;

/**
 * A StressTest that checks if the system is able to deal with concurrent increments on a transactional
 * integer. So there is a lot of contention.
 *
 * @author Peter Veentjer
 */
public class IsolationStressTest {

    private BetaStm stm;

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
        int threadCount = 2;
        UpdateThread[] threads = new UpdateThread[threadCount];
        BetaLongRef ref = BetaStmUtils.createLongRef(stm);
        long transactionsPerThread = 100 * 1000 * 1000;

        for (int k = 0; k < threads.length; k++) {
            threads[k] = new UpdateThread(k, ref, transactionsPerThread, pessimistic, dirtyCheckEnabled);
        }

        for (UpdateThread thread : threads) {
            thread.start();
        }

        joinAll(threads);
        long totalDurationMs = 0;
        for (UpdateThread thread : threads) {
            totalDurationMs += thread.durationMs;
        }

        System.out.println("--------------------------------------------------------");
        System.out.printf("Threadcount:       %s\n",threadCount);
        System.out.printf("Performance:       %s transactions/second/thread\n",
                transactionsPerSecondPerThreadAsString(transactionsPerThread, totalDurationMs, threadCount));
        System.out.printf("Performance:       %s transactions/second\n",
                transactionsPerSecondAsString(transactionsPerThread, totalDurationMs, threadCount));

        assertEquals(threadCount * transactionsPerThread, ref.___unsafeLoad().value);
        System.out.println("ref.orec: " + ref.___toOrecString());
    }

    class UpdateThread extends TestThread {
        private final boolean dirtyCheckEnabled;
        private final BetaLongRef ref;
        private final long count;
        private final boolean pessimistic;
        private long durationMs;

        public UpdateThread(int id, BetaLongRef ref, long count, boolean pessimistic, boolean dirtyCheckEnabled) {
            super("UpdateThread-" + id);
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

            AtomicVoidClosure closure = new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    btx.openForWrite(ref, pessimistic).value++;
                }
            };

            long startMs = currentTimeMillis();

            for (long k = 0; k < count; k++) {
                block.execute(closure);
            }

            durationMs = currentTimeMillis() - startMs;

            System.out.printf("finished %s after %s ms\n", getName(), durationMs);
        }
    }
}
