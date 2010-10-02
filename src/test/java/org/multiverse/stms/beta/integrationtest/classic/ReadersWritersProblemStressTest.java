package org.multiverse.stms.beta.integrationtest.classic;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.references.IntRef;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.StmUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class ReadersWritersProblemStressTest {

    private long count = 1000;
    private int readerThreadCount = 10;
    private int writerThreadCount = 5;
    private ReadersWritersLock readWriteLock;

    private AtomicLong currentReaderCount = new AtomicLong();
    private AtomicLong currentWriterCount = new AtomicLong();

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        readWriteLock = new ReadersWritersLock();
    }

    @Test
    public void test() {
        ReaderThread[] readers = createReaderThreads();
        WriterThread[] writers = createWriterThreads();

        startAll(writers);
        startAll(readers);
        joinAll(writers);
        joinAll(readers);

        assertEquals(0, currentReaderCount.get());
        assertEquals(0, currentWriterCount.get());
    }

    private ReaderThread[] createReaderThreads() {
        ReaderThread[] readers = new ReaderThread[readerThreadCount];
        for (int k = 0; k < readerThreadCount; k++) {
            readers[k] = new ReaderThread(k);
        }
        return readers;
    }

    private WriterThread[] createWriterThreads() {
        WriterThread[] writers = new WriterThread[writerThreadCount];
        for (int k = 0; k < writerThreadCount; k++) {
            writers[k] = new WriterThread(k);
        }
        return writers;
    }

    public class ReaderThread extends TestThread {

        public ReaderThread(int id) {
            super("ReaderThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            for (int k = 0; k < count; k++) {
                readWriteLock.acquireReadLock();
                try {
                    assertNoWriters();

                    currentReaderCount.incrementAndGet();
                    sleepRandomMs(2);
                    currentReaderCount.decrementAndGet();

                    assertNoWriters();

                    if (k % 1000 == 0) {
                        System.out.printf("%s is at count %s\n", getName(), k);
                    }
                } finally {
                    readWriteLock.releaseReadLock();
                }

                sleepRandomMs(5);
            }
        }
    }

    private void assertNoWriters() {
        if (currentWriterCount.get() > 0) {
            fail();
        }
    }

    public class WriterThread extends TestThread {

        public WriterThread(int id) {
            super("WriterThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            for (int k = 0; k < count; k++) {
                readWriteLock.acquireWriteLock();
                try {
                    assertNoReaders();
                    assertNoWriters();

                    currentWriterCount.incrementAndGet();
                    sleepRandomMs(20);
                    currentWriterCount.decrementAndGet();

                    assertNoWriters();
                    assertNoReaders();

                    if (k % 100 == 0) {
                        System.out.printf("%s is at count %s\n", getName(), k);
                    }
                } finally {
                    readWriteLock.releaseWriteLock();
                }
            }
        }

        private void assertNoReaders() {
            if (currentReaderCount.get() > 0) {
                fail();
            }
        }
    }

    static class ReadersWritersLock {

        //-1  is write lock, 0 = free, positive number is readLock count.
        private final IntRef readerCount = newIntRef();

        public void acquireReadLock() {
            execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    if (readerCount.get() == -1) {
                        retry();
                    }

                    readerCount.incrementAndGet(1);
                }
            });
        }

        public void acquireWriteLock() {
            execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    if (readerCount.get() != 0) {
                        retry();
                    }

                    readerCount.incrementAndGet(-1);
                }
            });
        }

        public void releaseWriteLock() {
            readerCount.atomicSet(0);
        }

        public void releaseReadLock() {
            readerCount.atomicIncrementAndGet(-1);
        }
    }


}
