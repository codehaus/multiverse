package org.multiverse.integrationtests.stability;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.api.Stm;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.sleepMs;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionRetriedOnFailureTest {
    private Stm stm;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = getGlobalStmInstance();
    }

    @Test
    public void whenReadConflict() {
        Ref ref = new Ref();

        DelayedReadThread t = new DelayedReadThread(ref);

        t.start();
        ref.set(100);

        joinAll(t);

        assertEquals(2, t.startedCount);
        assertEquals(100, t.read);
        assertEquals(1, t.readCount);
        assertEquals(100, ref.get());
    }

    @Test
    public void whenWriteConflict() {
        Ref ref = new Ref();

        DelayedWriteThread t = new DelayedWriteThread(ref, 200);

        t.start();
        ref.set(100);

        joinAll(t);

        //assertEquals(2, t.startedCount);
        //todo: why is this 1 and not 2 (no read conflict should be)
        assertEquals(1, t.writeCount);
        assertEquals(200, ref.get());
    }

    @Test
    public void whenRetry() {
        Ref ref = new Ref(0);

        BlockingReadThread t = new BlockingReadThread(ref, 1);
        t.start();

        sleepMs(500);
        ref.set(1);

        joinAll(t);

        assertEquals(1, ref.get());
        //assertEquals(2, t.startedCount);
        assertEquals(1, t.retries);
    }

    @Test
    @Ignore
    public void whenSpeculativeFailure() {

    }

    private static class BlockingReadThread extends TestThread {
        private final Ref ref;
        private final int expectedValue;
        private int startedCount;
        private int retries;

        public BlockingReadThread(Ref ref, int expectedValue) {
            this.ref = ref;
            this.expectedValue = expectedValue;
        }

        @Override
        @TransactionalMethod(readonly = true, trackReads = true)
        public void doRun() throws Exception {
            startedCount++;

            if (ref.get() != expectedValue) {
                retries++;
                retry();
            }

            //add
        }
    }

    private static class DelayedReadThread extends TestThread {
        private final Ref ref;
        private int startedCount = 0;
        private int read;
        private int readCount;

        public DelayedReadThread(Ref ref) {
            super("DelayedReadThread");
            this.ref = ref;
        }

        @Override
        @TransactionalMethod(readonly = false)
        public void doRun() throws Exception {
            startedCount++;
            sleepMs(500);
            read = ref.get();
            readCount++;
        }
    }

    private static class DelayedWriteThread extends TestThread {
        private final Ref ref;
        private int startedCount = 0;
        private int writeCount;
        private int update;

        public DelayedWriteThread(Ref ref, int update) {
            this.ref = ref;
            this.update = update;
        }

        @Override
        @TransactionalMethod(readonly = false)
        public void doRun() throws Exception {
            startedCount++;
            sleepMs(500);
            ref.set(update);
            writeCount++;
        }
    }

    @TransactionalObject
    static class Ref {
        private int value;

        public Ref() {
            this(0);
        }

        public Ref(int value) {
            this.value = value;
        }

        public int get() {
            return value;
        }

        public void set(int value) {
            this.value = value;
        }
    }
}
