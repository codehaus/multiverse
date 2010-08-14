package org.multiverse.stms.beta.integrationtest.isolation;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.TestUtils;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaTransactionFactory;
import org.multiverse.stms.beta.BetaTransactionTemplate;
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
            int k = 0;
            while (!stop) {
                write();
                sleepRandomUs(100);

                 k++;

                if(k % 100000 == 0){
                    System.out.printf("%s is at %s\n",getName(),k);
                }
            }
        }

        private void write() {
            final BetaObjectPool pool = getThreadLocalBetaObjectPool();

            new BetaTransactionTemplate(stm) {
                @Override
                public Object execute(BetaTransaction tx) throws Exception {
                    for (LongRef ref : refs) {
                        ref.set(tx, pool, ref.get(tx, pool));
                    }
                    return null;
                }
            }.execute();
        }
    }

    public class ReadThread extends TestThread {
        public ReadThread(int id) {
            super("ReadThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            int k = 0;
            while (!stop) {
                read();
                k++;

                if(k % 100000 == 0){
                    System.out.printf("%s is at %s\n",getName(),k);
                }
            }
        }

        public void read() {
            final BetaObjectPool pool = getThreadLocalBetaObjectPool();

            BetaTransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                    .setReadonly(true)
                    .build();

            new BetaTransactionTemplate(txFactory) {
                @Override
                public Object execute(BetaTransaction tx) throws Exception {

                    long initial = refs[0].get(tx, pool);

                    for (int k = 1; k < refs.length; k++) {
                        if (refs[k].get(tx,pool) != initial) {
                            fail();
                        }
                    }

                    return null;
                }
            }.execute();

        }
    }
}
