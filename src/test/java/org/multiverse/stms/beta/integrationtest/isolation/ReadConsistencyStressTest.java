package org.multiverse.stms.beta.integrationtest.isolation;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.TestUtils;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;
import static org.multiverse.stms.beta.ThreadLocalBetaObjectPool.getThreadLocalBetaObjectPool;

public class ReadConsistencyStressTest {

    private LongRef[] refs;

    private int readerCount = 10;
    private int writerCount = 2;
    private volatile boolean stop;
    private BetaStm stm;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stop = false;
        stm = new BetaStm();
    }

    @Test
    public void testRefCount_2() {
        test(2);
    }

    @Test
    public void testRefCount_4() {
        test(4);
    }

    @Test
    public void testRefCount_16() {
        test(16);
    }

    @Test
    public void testRefCount_64() {
        test(64);
    }

    @Test
    public void testRefCount_256() {
        test(256);
    }

    @Test
    public void testRefCount_1024() {
        test(1024);
    }

    public void test(int refCount) {

        refs = new LongRef[refCount];
        for (int k = 0; k < refs.length; k++) {
            refs[k] = createLongRef(stm);
        }

        ReadThread[] readerThreads = new ReadThread[readerCount];
        for (int k = 0; k < readerThreads.length; k++) {
            readerThreads[k] = new ReadThread(k);
        }

        WriterThread[] writerThreads = new WriterThread[writerCount];
        for (int k = 0; k < writerThreads.length; k++) {
            writerThreads[k] = new WriterThread(k);
        }

        startAll(readerThreads);
        startAll(writerThreads);
        sleepMs(TestUtils.getStressTestDurationMs(30 * 1000));
        stop = true;
        joinAll(readerThreads);
        joinAll(writerThreads);
    }

    public class WriterThread extends TestThread {

        public WriterThread(int id) {
            super("WriterThread-" + id);
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
                    for (LongRef ref : refs) {
                        ref.set(btx, pool, ref.get(btx, pool));
                    }
                }
            };

            int k = 0;
            while (!stop) {
                block.execute(closure);
                sleepRandomUs(100);

                k++;

                if (k % 500000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }
        }
    }

    public class ReadThread extends TestThread {
        public ReadThread(int id) {
            super("ReadThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            AtomicBlock block = stm.getTransactionFactoryBuilder()
                    .setReadonly(true)
                    .buildAtomicBlock();
            AtomicVoidClosure closure = new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    BetaObjectPool pool = getThreadLocalBetaObjectPool();

                    long initial = refs[0].get(btx, pool);

                    for (int k = 1; k < refs.length; k++) {
                        if (refs[k].get(btx, pool) != initial) {
                            fail();
                        }
                    }
                }
            };

            int k = 0;
            while (!stop) {
                block.execute(closure);
                k++;

                if (k % 100000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }
        }
    }
}
