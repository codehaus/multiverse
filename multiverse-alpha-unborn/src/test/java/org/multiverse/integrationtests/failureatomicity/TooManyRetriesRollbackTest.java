package org.multiverse.integrationtests.failureatomicity;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.exceptions.TooManyRetriesException;
import org.multiverse.transactional.refs.IntRef;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TooManyRetriesRollbackTest {
    private IntRef modifyRef;
    private IntRef retryRef;
    private volatile boolean finished;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();

        modifyRef = new IntRef();
        retryRef = new IntRef();
        finished = false;
    }

    @Test
    public void test() {
        NotifyThread notifyThread = new NotifyThread();
        notifyThread.start();

        try {
            setAndAwaitUneven(1);
            fail();
        } catch (TooManyRetriesException expected) {
        }

        finished = true;
        assertEquals(0, modifyRef.get());
        joinAll(notifyThread);
    }

    @TransactionalMethod(maxRetries = 10)
    public void setAndAwaitUneven(int value) {
        modifyRef.set(value);

        if (retryRef.get() % 2 == 0) {
            retry();
        }
    }

    class NotifyThread extends TestThread {

        public NotifyThread() {
            super("NotifyThread");
        }

        @Override
        public void doRun() throws Exception {
            while (!finished) {
                retryRef.set(retryRef.get() + 2);
            }
        }
    }
}
