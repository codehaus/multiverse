package org.multiverse.stms.beta.integrationtest.isolation;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.benchmarks.BenchmarkUtils;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmUtils;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
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
        long txCount = 100 * 1000 * 1000;

        for (int k = 0; k < threads.length; k++) {
            threads[k] = new UpdateThread(k, ref, txCount, pessimistic, dirtyCheckEnabled);
        }

        for (UpdateThread thread : threads) {
            thread.start();
        }

        joinAll(threads);
        long durationMs = 0;
        for (UpdateThread thread : threads) {
            durationMs += thread.durationMs;
        }

        double performance = BenchmarkUtils.perSecond(txCount, durationMs, threadCount);
        System.out.printf("Performance %s transactions/second\n", BenchmarkUtils.format(performance));
        assertEquals(threadCount * txCount, ref.___unsafeLoad().value);
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
            final BetaObjectPool pool = new BetaObjectPool();

            AtomicBlock block = stm.getTransactionFactoryBuilder()
                    .setDirtyCheckEnabled(dirtyCheckEnabled)
                    .buildAtomicBlock();

            AtomicVoidClosure closure = new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    LongRefTranlocal tranlocal = btx.openForWrite(ref, pessimistic, pool);
                    tranlocal.value++;
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
