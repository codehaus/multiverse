package org.multiverse.api;

import org.junit.Test;
import org.multiverse.TestThread;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.api.ThreadLocalTransaction.*;

/**
 * @author Peter Veentjer
 */
public class ThreadLocalTransactionTest {

    @Test
    public void clear() {
        Transaction t = mock(Transaction.class);
        setThreadLocalTransaction(t);

        ThreadlocalThread threadlocalThread = new ThreadlocalThread();
        startAll(threadlocalThread);
        joinAll(threadlocalThread);


        assertSame(t, getThreadLocalTransaction());
    }

    class ThreadlocalThread extends TestThread {

        @Override
        public void doRun() throws Exception {
            Transaction t = mock(Transaction.class);
            setThreadLocalTransaction(t);
            clearThreadLocalTransaction();
            assertNull(getThreadLocalTransaction());
        }
    }
}
