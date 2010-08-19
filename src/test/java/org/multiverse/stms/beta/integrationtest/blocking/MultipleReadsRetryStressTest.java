package org.multiverse.stms.beta.integrationtest.blocking;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.TestUtils;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.stms.beta.*;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.BetaTransactionConfiguration;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

public class MultipleReadsRetryStressTest {
    private BetaStm stm;
    private LongRef[] refs;
    private LongRef stopRef;
    private volatile boolean stop;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = new BetaStm();
        stop = false;
        stopRef = createLongRef(stm, 0);
    }

    @Test
    public void withArrayTreeTransactionAnd2Threads() throws InterruptedException {
        FatArrayTreeBetaTransactionFactory txFactory = new FatArrayTreeBetaTransactionFactory(stm);
        test(new LeanBetaAtomicBlock(txFactory), 10, 2);
    }

    @Test
    public void withArrayTransactionAnd2Threads() throws InterruptedException {
        int refCount = 10;
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, refCount + 1);
        FatArrayBetaTransactionFactory txFactory = new FatArrayBetaTransactionFactory(config);
        test(new LeanBetaAtomicBlock(txFactory), refCount, 2);
    }

    @Test
    public void withArrayTreeTransactionAnd5Threads() throws InterruptedException {
        FatArrayTreeBetaTransactionFactory txFactory = new FatArrayTreeBetaTransactionFactory(stm);
        test(new LeanBetaAtomicBlock(txFactory), 10, 5);
    }

    @Test
    public void withArrayTransactionAnd5Threads() throws InterruptedException {
        int refCount = 10;
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, refCount + 1);
        FatArrayBetaTransactionFactory txFactory = new FatArrayBetaTransactionFactory(config);
        test(new LeanBetaAtomicBlock(txFactory), refCount, 5);
    }

    public void test(AtomicBlock atomicBlock, int refCount, int threadCount) throws InterruptedException {
        refs = new LongRef[refCount];
        for (int k = 0; k < refs.length; k++) {
            refs[k] = createLongRef(stm);
        }

        UpdateThread[] threads = new UpdateThread[threadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new UpdateThread(k, atomicBlock, threadCount);
        }

        startAll(threads);

        sleepMs(30 * 1000);
        stop = true;

        new BetaTransactionTemplate(stm) {
            @Override
            public Object execute(BetaTransaction tx) {
                tx.openForWrite(stopRef, false, new BetaObjectPool()).value = -1;
                return null;
            }
        }.execute(new BetaObjectPool());

        System.out.println("Waiting for joining threads");

        joinAll(threads);

        assertEquals(sumCount(threads), sumRefs());
    }

    private long sumRefs() {
        long result = 0;
        for (LongRef ref : refs) {
            result += ref.___unsafeLoad().value;
        }
        return result;
    }

    private long sumCount(UpdateThread[] threads) {
        long result = 0;
        for (UpdateThread thread : threads) {
            result += thread.count;
        }
        return result;
    }

    private class UpdateThread extends TestThread {

        private final AtomicBlock atomicBlock;
        private final int id;
        private final int threadCount;
        private long count;

        public UpdateThread(int id, AtomicBlock atomicBlock, int threadCount) {
            super("UpdateThread-" + id);
            this.atomicBlock = atomicBlock;
            this.id = id;
            this.threadCount = threadCount;
        }

        @Override
        public void doRun() {
            final BetaObjectPool pool = new BetaObjectPool();

            AtomicVoidClosure closure = new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;

                    if (btx.openForRead(stopRef, false, pool).value < 0) {
                        throw new StopException();
                    }

                    long sum = 0;
                    for (LongRef ref : refs) {
                        sum += btx.openForRead(ref, false, pool).value;
                    }

                    if (sum % threadCount != id) {
                        retry();
                    }

                    LongRef ref = refs[TestUtils.randomInt(refs.length)];
                    btx.openForWrite(ref, false, pool).value++;
                }

            };

            while (!stop) {
                if (count % (100000) == 0) {
                    System.out.println(getName() + " " + count);
                }

                try {
                    atomicBlock.execute(closure);
                } catch (StopException e) {
                    break;
                }

                count++;
            }
        }
    }

    class StopException extends RuntimeException {
    }
}
