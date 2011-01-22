package org.multiverse.stms.gamma.integration.isolation;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.LockMode;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.gamma.benchmarks.BenchmarkUtils.transactionsPerSecondAsString;
import static org.multiverse.stms.gamma.benchmarks.BenchmarkUtils.transactionsPerSecondPerThreadAsString;

/**
 * A StressTest that checks if the system is able to deal with concurrent increments on a LongRef
 * So there is a lot of contention.
 *
 * @author Peter Veentjer
 */
public abstract class Isolation_AbstractTest implements GammaConstants {

    public long transactionsPerThread = 100 * 1000 * 1000;
    public final int threadCount = 2;

    protected GammaStm stm;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = (GammaStm) getGlobalStmInstance();
    }

    protected abstract AtomicBlock newBlock(LockMode lockMode, boolean dirtyCheckEnabled);

    @Test
    public void withNoLockingDirtyCheck() {
        test(LockMode.None, true);
    }

    @Test
    public void withNoLockAndNoDirtyCheck() {
        test(LockMode.None, false);
    }

    @Test
    public void withReadLockAndDirtyCheck() {
        test(LockMode.Read, true);
    }

    @Test
    public void withReadLockAndNoDirtyCheck() {
        test(LockMode.Read, false);
    }

    @Test
    public void withWriteLockingAndDirtyCheck() {
        test(LockMode.Write, true);
    }

    @Test
    public void withWriteLockAndNoDirtyCheck() {
        test(LockMode.Write, false);
    }

    @Test
    public void withExclusiveLockAndDirtyCheck() {
        test(LockMode.Exclusive, true);
    }

    @Test
    public void withExclusiveLockNoDirtyCheck() {
        test(LockMode.Exclusive, false);
    }

    @Test
    public void withMixedSettings() {
        transactionsPerThread = 10000000;

        GammaLongRef ref = new GammaLongRef(stm);

        UpdateThread[] threads = new UpdateThread[8];
        threads[0] = new UpdateThread(0, ref, LockMode.None, true);
        threads[1] = new UpdateThread(1, ref, LockMode.None, false);
        threads[2] = new UpdateThread(0, ref, LockMode.Read, true);
        threads[3] = new UpdateThread(1, ref, LockMode.Read, false);
        threads[4] = new UpdateThread(2, ref, LockMode.Write, true);
        threads[5] = new UpdateThread(3, ref, LockMode.Write, false);
        threads[6] = new UpdateThread(4, ref, LockMode.Exclusive, true);
        threads[7] = new UpdateThread(5, ref, LockMode.Exclusive, false);

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

    public void test(LockMode lockMode, boolean dirtyCheckEnabled) {
        UpdateThread[] threads = new UpdateThread[threadCount];
        GammaLongRef ref = new GammaLongRef(stm);

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
        private final GammaLongRef ref;
        private final LockMode lockMode;
        private long durationMs;

        public UpdateThread(int id, GammaLongRef ref, LockMode lockMode, boolean dirtyCheckEnabled) {
            super("UpdateThread-" + id);
            this.ref = ref;
            this.lockMode = lockMode;
            this.dirtyCheckEnabled = dirtyCheckEnabled;
        }

        @Override
        public void doRun() {
            AtomicBlock block = newBlock(lockMode, dirtyCheckEnabled);

            AtomicVoidClosure closure = new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    GammaTransaction btx = (GammaTransaction) tx;
                    btx.arriveEnabled = false;
                    ref.openForWrite(btx, LOCKMODE_NONE).long_value++;
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
