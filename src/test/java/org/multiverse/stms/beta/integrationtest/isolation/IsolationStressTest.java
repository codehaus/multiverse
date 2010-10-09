package org.multiverse.stms.beta.integrationtest.isolation;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
import static org.multiverse.stms.beta.benchmarks.BenchmarkUtils.transactionsPerSecondAsString;
import static org.multiverse.stms.beta.benchmarks.BenchmarkUtils.transactionsPerSecondPerThreadAsString;

/**
 * A StressTest that checks if the system is able to deal with concurrent increments on a LongRef
 * So there is a lot of contention.
 *
 * @author Peter Veentjer
 */
public class IsolationStressTest implements BetaStmConstants {

    public long transactionsPerThread = 100 * 1000 * 1000;
    public final int threadCount = 2;

    private BetaStm stm;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = new BetaStm();
    }

    @Test
    public void withNoLockingAndDirtyCheck() {
        test(LOCKMODE_NONE, true);
    }

    @Test
    public void withUpdateLockSettingsAndDirtyCheck() {
        test(LOCKMODE_UPDATE, true);
    }

    @Test
    public void withCommitLockSettingsAndDirtyCheck() {
        test(LOCKMODE_COMMIT, true);
    }

    @Test
    public void withUpdateLockNoDirtyCheck() {
        test(LOCKMODE_UPDATE, false);
    }

    @Test
    public void withNoLockingNoDirtyCheck() {
        test(LOCKMODE_NONE, false);
    }

    @Test
    public void withCommitLockAndNoDirtyCheck() {
        test(LOCKMODE_COMMIT, false);
    }

    @Test
    public void withMixedSettings() {
        transactionsPerThread = 10000000;

        BetaLongRef ref = newLongRef(stm);

        UpdateThread[] threads = new UpdateThread[6];
        threads[0] = new UpdateThread(0, ref, LOCKMODE_NONE, true);
        threads[1] = new UpdateThread(1, ref, LOCKMODE_NONE, false);
        threads[2] = new UpdateThread(2, ref, LOCKMODE_UPDATE, true);
        threads[3] = new UpdateThread(3, ref, LOCKMODE_UPDATE, false);
        threads[4] = new UpdateThread(4, ref, LOCKMODE_COMMIT, true);
        threads[5] = new UpdateThread(5, ref, LOCKMODE_COMMIT, false);

        startAll(threads);

        joinAll(threads);
        long totalDurationMs = 0;
        for (UpdateThread thread : threads) {
            totalDurationMs += thread.durationMs;
        }

        System.out.println("--------------------------------------------------------");
        System.out.printf("Threadcount:       %s\n", threads.length);
        System.out.printf("Performance:       %s transactions/second/thread\n",
                transactionsPerSecondPerThreadAsString(transactionsPerThread, totalDurationMs, threads.length));
        System.out.printf("Performance:       %s transactions/second\n",
                transactionsPerSecondAsString(transactionsPerThread, totalDurationMs, threads.length));

        assertEquals(threads.length * transactionsPerThread, ref.atomicGet());
        System.out.println("ref.orec: " + ref.___toOrecString());
    }

    public void test(int lockMode, boolean dirtyCheckEnabled) {
        UpdateThread[] threads = new UpdateThread[threadCount];
        BetaLongRef ref = newLongRef(stm);

        for (int k = 0; k < threads.length; k++) {
            threads[k] = new UpdateThread(k, ref, lockMode, dirtyCheckEnabled);
        }

        startAll(threads);

        joinAll(threads);
        long totalDurationMs = 0;
        for (UpdateThread thread : threads) {
            totalDurationMs += thread.durationMs;
        }

        System.out.println("--------------------------------------------------------");
        System.out.printf("Threadcount:       %s\n", threadCount);
        System.out.printf("Performance:       %s transactions/second/thread\n",
                transactionsPerSecondPerThreadAsString(transactionsPerThread, totalDurationMs, threadCount));
        System.out.printf("Performance:       %s transactions/second\n",
                transactionsPerSecondAsString(transactionsPerThread, totalDurationMs, threadCount));

        assertEquals(threadCount * transactionsPerThread, ref.atomicGet());
        System.out.println("ref.orec: " + ref.___toOrecString());
    }

    class UpdateThread extends TestThread {
        private final boolean dirtyCheckEnabled;
        private final BetaLongRef ref;
        private final int lockMode;
        private long durationMs;

        public UpdateThread(int id, BetaLongRef ref, int lockMode, boolean dirtyCheckEnabled) {
            super("UpdateThread-" + id);
            this.ref = ref;
            this.lockMode = lockMode;
            this.dirtyCheckEnabled = dirtyCheckEnabled;
        }

        @Override
        public void doRun() {
            AtomicBlock block = stm.createTransactionFactoryBuilder()
                    .setDirtyCheckEnabled(dirtyCheckEnabled)
                    .setMaxRetries(10000)
                    .buildAtomicBlock();

            AtomicVoidClosure closure = new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    btx.openForWrite(ref, lockMode).value++;
                }
            };

            long startMs = currentTimeMillis();

            for (long k = 0; k < transactionsPerThread; k++) {
                block.execute(closure);

                if (k % 500000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }

            durationMs = currentTimeMillis() - startMs;

            System.out.printf("finished %s after %s ms\n", getName(), durationMs);
        }
    }
}
