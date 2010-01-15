package org.multiverse.utils;

import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.Transaction;
import org.multiverse.transactional.primitives.TransactionalInteger;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class CommitGroup_commitTest {

    @Test
    public void whenNoPendingTransactions() {
        CommitGroup group = new CommitGroup();
        group.commit();

        assertTrue(group.isCommitted());
    }

    @Test
    public void whenPendingTransactions() {
        CommitGroup group = new CommitGroup();

        TransactionalInteger ref1 = new TransactionalInteger();
        TransactionalInteger ref2 = new TransactionalInteger();
        TransactionalInteger ref3 = new TransactionalInteger();

        IncThread thread1 = new IncThread(ref1, group);
        IncThread thread2 = new IncThread(ref2, group);
        IncThread thread3 = new IncThread(ref3, group);

        startAll(thread1, thread2, thread3);

        sleepMs(500);
        group.commit();
        joinAll(thread1, thread2, thread3);

        assertIsCommitted(thread1.tx);
        assertIsCommitted(thread2.tx);
        assertIsCommitted(thread3.tx);

        assertEquals(1, ref1.get());
        assertEquals(1, ref2.get());
        assertEquals(1, ref3.get());
    }

    @Test
    public void whenGroupCommitted_thenIgnored() {
        CommitGroup group = new CommitGroup();
        group.commit();

        group.commit();
        assertTrue(group.isCommitted());
    }

    @Test
    public void whenGroupAborted_thenIllegalStateException() {
        CommitGroup group = new CommitGroup();
        group.abort();

        try {
            group.commit();
            fail();
        } catch (IllegalStateException expected) {
        }
        assertTrue(group.isAborted());
    }

    public class IncThread extends TestThread {
        private final TransactionalInteger ref;
        private final CommitGroup group;
        private Transaction tx;

        public IncThread(TransactionalInteger ref, CommitGroup group) {
            super("IncThread");
            this.group = group;
            this.ref = ref;
        }

        @Override
        @TransactionalMethod
        public void doRun() throws Exception {
            tx = getThreadLocalTransaction();
            ref.inc();
            group.awaitCommit(tx);
        }
    }
}
