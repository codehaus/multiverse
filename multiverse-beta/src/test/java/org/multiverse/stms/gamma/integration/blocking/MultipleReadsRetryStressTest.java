package org.multiverse.stms.gamma.integration.blocking;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.TestUtils;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.stms.gamma.*;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactions.GammaTransactionConfiguration;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class MultipleReadsRetryStressTest implements GammaConstants {
    private GammaStm stm;
    private GammaLongRef[] refs;
    private GammaLongRef stopRef;
    private volatile boolean stop;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = (GammaStm) getGlobalStmInstance();
        stop = false;
        stopRef = new GammaLongRef(stm, 0);
    }

    @Test
    public void withMapTransactionAnd2Threads() throws InterruptedException {
        MapGammaTransactionFactory txFactory = new MapGammaTransactionFactory(stm);
        test(new LeanGammaAtomicBlock(txFactory), 10, 2);
    }

    @Test
    public void withArrayTransactionAnd2Threads() throws InterruptedException {
        int refCount = 10;
        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm, refCount + 1);
        ArrayGammaTransactionFactory txFactory = new ArrayGammaTransactionFactory(config);
        test(new LeanGammaAtomicBlock(txFactory), refCount, 2);
    }

    @Test
    public void withMapTransactionAnd5Threads() throws InterruptedException {
        MapGammaTransactionFactory txFactory = new MapGammaTransactionFactory(stm);
        test(new LeanGammaAtomicBlock(txFactory), 10, 5);
    }

    @Test
    public void withArrayTransactionAnd5Threads() throws InterruptedException {
        int refCount = 10;
        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm, refCount + 1);
        ArrayGammaTransactionFactory txFactory = new ArrayGammaTransactionFactory(config);
        test(new LeanGammaAtomicBlock(txFactory), refCount, 5);
    }

    public void test(AtomicBlock atomicBlock, int refCount, int threadCount) throws InterruptedException {
        refs = new GammaLongRef[refCount];
        for (int k = 0; k < refs.length; k++) {
            refs[k] = new GammaLongRef(stm);
        }

        UpdateThread[] threads = new UpdateThread[threadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new UpdateThread(k, atomicBlock, threadCount);
        }

        startAll(threads);

        sleepMs(30 * 1000);
        stop = true;

        stopRef.atomicSet(-1);

        System.out.println("Waiting for joining threads");

        joinAll(threads);

        assertEquals(sumCount(threads), sumRefs());
    }

    private long sumRefs() {
        long result = 0;
        for (GammaLongRef ref : refs) {
            result += ref.atomicGet();
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
            AtomicVoidClosure closure = new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx)  {
                     if (stopRef.get() < 0) {
                        throw new StopException();
                    }

                    long sum = 0;
                    for (GammaLongRef ref : refs) {
                        sum += ref.get();
                    }

                    if (sum % threadCount != id) {
                        retry();
                    }

                    GammaLongRef ref = refs[TestUtils.randomInt(refs.length)];
                    ref.incrementAndGet(1);
                }

            };

            while (!stop) {
                if (count % (10000) == 0) {
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
