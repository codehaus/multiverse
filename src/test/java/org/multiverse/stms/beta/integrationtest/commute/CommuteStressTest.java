package org.multiverse.stms.beta.integrationtest.commute;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicLongClosure;
import org.multiverse.functions.IncLongFunction;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.FatArrayTreeBetaTransactionFactory;
import org.multiverse.stms.beta.LeanBetaAtomicBlock;
import org.multiverse.stms.beta.transactionalobjects.LongRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.BetaTransactionConfiguration;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;
import static org.multiverse.stms.beta.ThreadLocalBetaObjectPool.getThreadLocalBetaObjectPool;

public class CommuteStressTest {
     private BetaStm stm;
    private volatile boolean stop;
    private LongRef[] refs;
    private int refCount = 10;
    private int workerCount = 2;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = new BetaStm();
        stop = false;
    }

    @Test
    public void test() {
        refs = new LongRef[refCount];
        for (int k = 0; k < refCount; k++) {
            refs[k] = createLongRef(stm);
        }

        WorkerThread[] workers = new WorkerThread[workerCount];
        for (int k = 0; k < workers.length; k++) {
            workers[k] = new WorkerThread(k);
        }

        startAll(workers);
        sleepMs(getStressTestDurationMs(30 * 1000));
        stop = true;
        joinAll(workers);

        assertEquals(count(workers), count(refs));
    }

    public long count(LongRef[] refs) {
        long result = 0;
        for (LongRef ref : refs) {
            result += ref.___unsafeLoad().value;
        }
        return result;
    }

    public long count(WorkerThread[] threads) {
        long result = 0;
        for (WorkerThread thread : threads) {
            result += thread.count;
        }
        return result;
    }

    public class WorkerThread extends TestThread {

        private long count;

        public WorkerThread(int id) {
            super("CommuteThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm);
            AtomicBlock block = new LeanBetaAtomicBlock(new FatArrayTreeBetaTransactionFactory(config));

            AtomicLongClosure commutingClosure = new AtomicLongClosure() {
                @Override
                public long execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    BetaObjectPool pool = getThreadLocalBetaObjectPool();
                    for (int k = 0; k < refs.length; k++) {
                        btx.commute(refs[k], pool, IncLongFunction.INSTANCE);
                    }
                    return refs.length;
                }
            };

            AtomicLongClosure nonCommutingClosure = new AtomicLongClosure() {
                @Override
                public long execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    BetaObjectPool pool = getThreadLocalBetaObjectPool();
                    for (int k = 0; k < refs.length; k++) {
                        btx.openForWrite(refs[k], false, pool).value++;
                    }
                    return refs.length;
                }
            };

            int k = 0;
            while (!stop) {
                AtomicLongClosure closure = randomOneOf(10) ? nonCommutingClosure : commutingClosure;
                count += block.execute(closure);
                k++;

                if (k % 100000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }

            System.out.printf("%s completed %s\n", getName(), k);
        }
    }
}