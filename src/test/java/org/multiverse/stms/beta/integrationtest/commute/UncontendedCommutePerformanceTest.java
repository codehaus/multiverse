package org.multiverse.stms.beta.integrationtest.commute;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.functions.IncLongFunction;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.LongRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.benchmarks.BenchmarkUtils.transactionsPerSecond;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;
import static org.multiverse.stms.beta.ThreadLocalBetaObjectPool.getThreadLocalBetaObjectPool;

public class UncontendedCommutePerformanceTest {
    private volatile boolean stop;
    private BetaStm stm;
    private LongRef ref;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = new BetaStm();
        ref = createLongRef(stm);
    }

    @Test
    public void testNormalIncrement() {
        NormalIncThread thread = new NormalIncThread();

        startAll(thread);
        long durationMs = getStressTestDurationMs(30 * 1000);
        sleepMs(durationMs);
        stop = true;
        joinAll(thread);

        long transactionCount = ref.___unsafeLoad().value;
        String performance = transactionsPerSecond(transactionCount, durationMs);
        System.out.println(performance + " Transactions/second");
    }

    @Test
    public void testCommuteIncrement() {
        CommuteIncThread thread = new CommuteIncThread();

        startAll(thread);
        long durationMs = getStressTestDurationMs(30 * 1000);
        sleepMs(durationMs);
        stop = true;
        joinAll(thread);

        long transactionCount = ref.___unsafeLoad().value;
        String performance = transactionsPerSecond(transactionCount, durationMs);
        System.out.println(performance + " Transactions/second");
    }

    public class NormalIncThread extends TestThread {
        public NormalIncThread() {
            super("NormalIncThread");
        }

        @Override
        public void doRun() throws Exception {
            AtomicBlock block = stm.getTransactionFactoryBuilder()
                    .setPessimisticLockLevel(PessimisticLockLevel.Read)
                    .setDirtyCheckEnabled(false)
                    .buildAtomicBlock();

            AtomicVoidClosure closure = new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    BetaObjectPool pool = getThreadLocalBetaObjectPool();
                    btx.openForWrite(ref, false, pool).value++;
                }
            };

            while (!stop) {
                block.execute(closure);
            }
        }
    }

    public class CommuteIncThread extends TestThread {
        public CommuteIncThread() {
            super("CommuteIncThread");
        }

        @Override
        public void doRun() throws Exception {
            AtomicBlock block = stm.getTransactionFactoryBuilder()
                    .buildAtomicBlock();

            AtomicVoidClosure closure = new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    BetaObjectPool pool = getThreadLocalBetaObjectPool();
                    btx.commute(ref, pool, IncLongFunction.INSTANCE);
                }
            };

            while (!stop) {
                block.execute(closure);
            }
        }
    }
}
