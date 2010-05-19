package org.multiverse.integrationtests.isolation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.transactional.refs.LongRef;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * Checks if the reads are all consistent to the beginning of the transaction.
 *
 * @author Peter Veentjer
 */
public class ReadConsistencyStressTest {
    private int refCount = 1000;
    private LongRef[] refs;
    private final AtomicBoolean shutdown = new AtomicBoolean();
    private int readerCount = 10;
    private int writerCount = 2;
    private int readTransactionCount = 1000 * 1000;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();

        refs = new LongRef[refCount];
        for (int k = 0; k < refs.length; k++) {
            refs[k] = new LongRef();
        }

        shutdown.set(false);
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void test() {
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
        joinAll(readerThreads);
        joinAll(writerThreads);
    }

    public class WriterThread extends TestThread {

        public WriterThread(int id) {
            super("WriterThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            while (!shutdown.get()) {
                write();
                sleepRandomMs(10);
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
            for (int k = 0; k < readTransactionCount; k++) {
                read();

                if (k % 100000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }

            shutdown.set(true);
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
