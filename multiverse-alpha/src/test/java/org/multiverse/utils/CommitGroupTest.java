package org.multiverse.utils;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.transactional.primitives.TransactionalInteger;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class CommitGroupTest {

    private CommitGroup commitGroup;

    @Before
    public void setUp() {
        commitGroup = new CommitGroup();
    }

    @Test
    public void test() throws InterruptedException {
        TransactionalInteger ref1 = new TransactionalInteger();
        TransactionalInteger ref2 = new TransactionalInteger();

        CommitThread t1 = new CommitThread(1, ref1);
        CommitThread t2 = new CommitThread(2, ref2);

        startAll(t1, t2);
        sleepMs(500);

        commitGroup.commit();

        joinAll(t1, t2);

        assertEquals(1, ref1.get());
        assertEquals(1, ref2.get());
    }

    @Test
    public void testAbort() throws InterruptedException {
        TransactionalInteger ref1 = new TransactionalInteger();
        TransactionalInteger ref2 = new TransactionalInteger();

        CommitThread t1 = new CommitThread(1, ref1);
        CommitThread t2 = new CommitThread(2, ref2);

        startAll(t1, t2);
        sleepMs(500);

        //assertEquals(0, ref1.get());
        //assertEquals(0, ref2.get());

        commitGroup.abort();

        t1.join();
        t2.join();
        assertEquals(0, ref1.get());
        assertEquals(0, ref2.get());
    }

    public class CommitThread extends TestThread {
        private TransactionalInteger ref;

        public CommitThread(int id, TransactionalInteger ref) {
            super("CommitThread-" + id);
            this.ref = ref;
        }

        @Override
        @TransactionalMethod
        public void doRun() throws Exception {
            ref.inc();
            commitGroup.awaitCommit(getThreadLocalTransaction());
        }
    }
}
