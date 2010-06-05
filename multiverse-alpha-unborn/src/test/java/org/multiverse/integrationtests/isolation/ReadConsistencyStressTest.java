package org.multiverse.integrationtests.isolation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.TestUtils;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.transactional.refs.LongRef;

import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * Checks if the reads are all consistent to the beginning of the transaction.
 *
 * @author Peter Veentjer
 */
public class ReadConsistencyStressTest {
    private LongRef[] refs;

    private int readerCount = 10;
    private int writerCount = 2;
    private volatile boolean stop;

    @Before
    public void setUp() {
        stop = false;
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void testRefCount_2(){
        test(2);
    }

    @Test
    public void testRefCount_4(){
        test(4);
    }

    @Test
    public void testRefCount_16(){
        test(16);
    }

    @Test
    public void testRefCount_64(){
        test(64);
    }

    @Test
    public void testRefCount_256(){
        test(256);
    }

    @Test
    public void testRefCount_1024(){
        test(1024);
    }

    public void test(int refCount) {

        refs = new LongRef[refCount];
        for (int k = 0; k < refs.length; k++) {
            refs[k] = new LongRef();
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
        sleepMs(TestUtils.getStressTestDurationMs(60 * 1000));
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
            while (!stop) {
                write();
                sleepRandomUs(100);
            }
        }

        @TransactionalMethod(readonly = false)
        private void write() {
            for (LongRef ref : refs) {
                ref.inc();
            }
        }
    }

    public class ReadThread extends TestThread {
        public ReadThread(int id) {
            super("ReadThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            while (!stop) {
                read();
            }
        }

        @TransactionalMethod(readonly = true)
        public void read() {
            long initial = refs[0].get();

            for (int k = 1; k < refs.length; k++) {
                if (refs[k].get() != initial) {
                    fail();
                }
            }
        }
    }
}
