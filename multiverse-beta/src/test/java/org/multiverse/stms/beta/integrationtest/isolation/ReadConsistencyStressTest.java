package org.multiverse.stms.beta.integrationtest.isolation;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.TestUtils;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;

public class ReadConsistencyStressTest {

    private BetaLongRef[] refs;

    private int readerCount = 10;
    private int writerCount = 2;
    private volatile boolean stop;
    private BetaStm stm;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stop = false;
        stm = (BetaStm) getGlobalStmInstance();
    }

    @Test
    public void with2RefsAndReadTracking() {
        test(2, true);
    }

    @Test
    public void with2RefsAndNoReadTracking() {
        test(2, false);
    }

    @Test
    public void with4RefsAndReadTracking() {
        test(4, true);
    }

    @Test
    public void with4RefsAndNoReadTracking() {
        test(4, false);
    }

    @Test
    public void with16RefsAndReadTracking() {
        test(16, true);
    }

    @Test
    public void with16RefsAndNoReadTracking() {
        test(16, false);
    }

    @Test
    public void with64RefsAndReadTracking() {
        test(64, true);
    }

    @Test
    public void with64RefsAndNoReadTracking() {
        test(64, false);
    }

    @Test
    public void with256RefsAndReadTracking() {
        test(256, true);
    }

    @Test
    public void with256RefsAndNoReadTracking() {
        test(256, false);
    }

    @Test
    public void with1024RefsAndReadTracking() {
        test(1024, true);
    }

    @Test
    public void with1024RefsAndNoReadTracking() {
        test(1024, false);
    }

    @Test
    public void with4096RefsAndReadTracking() {
        test(4096, true);
    }

    @Test
    public void with4096RefsAndNoReadTracking() {
        test(4096, false);
    }

    public void test(int refCount, boolean readtracking) {

        refs = new BetaLongRef[refCount];
        for (int k = 0; k < refs.length; k++) {
            refs[k] = newLongRef(stm);
        }

        ReadThread[] readerThreads = new ReadThread[readerCount];
        for (int k = 0; k < readerThreads.length; k++) {
            readerThreads[k] = new ReadThread(k, readtracking);
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
            AtomicBlock block = stm.createTransactionFactoryBuilder()
                    .buildAtomicBlock();
            AtomicVoidClosure closure = new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    for (BetaLongRef ref : refs) {
                        ref.set(btx, ref.get(btx));
                    }
                }
            };

            int mod = 1;
            int k = 0;
            while (!stop) {
                block.execute(closure);
                sleepRandomUs(100);

                k++;

                if (k % mod == 0) {
                    mod = mod * 10;
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }
        }
    }

    public class ReadThread extends TestThread {
        private final boolean readTracking;

        public ReadThread(int id, boolean readTracking) {
            super("ReadThread-" + id);
            this.readTracking = readTracking;
        }

        @Override
        public void doRun() throws Exception {
            AtomicBlock block = stm.createTransactionFactoryBuilder()
                    .setReadTrackingEnabled(readTracking)
                    .setBlockingAllowed(false)
                    .setReadonly(true)
                    .buildAtomicBlock();

            AtomicVoidClosure closure = new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;

                    long initial = refs[0].get(btx);

                    for (int k = 1; k < refs.length; k++) {
                        if (refs[k].get(btx) != initial) {
                            fail();
                        }
                    }
                }
            };

            int mod = 1;
            int k = 0;
            while (!stop) {
                block.execute(closure);
                k++;

                if (k % mod == 0) {
                    mod = mod * 10;
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }
        }
    }
}
