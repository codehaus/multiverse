package org.multiverse.integrationtests.failureatomicity;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.transactional.refs.IntRef;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

/**
 * The FailureAtomicityStressTest tests if failure atomicity works. So it should be impossible that the eventual
 * state is based on transactions that are eventually rolled back.
 * <p/>
 * There is a shared IntRef that is concurrently increased. Half of the transactions is going to be aborted
 * so with a writecount of x and a modifyThreadCount of y, the IntRef.getClassMetadata() will be x*y/2.
 *
 * @author Peter Veentjer.
 */
public class FailureAtomicityStressTest {

    private int modifyThreadCount = 10;
    private boolean stop;
    private IntRef ref;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        ref = new IntRef();
        stop = false;
    }

    @Test
    public void test() {
        ModifyThread[] modifyThreads = new ModifyThread[modifyThreadCount];
        for (int k = 0; k < modifyThreadCount; k++) {
            modifyThreads[k] = new ModifyThread(k);
        }

        startAll(modifyThreads);

        sleepMs(getStressTestDurationMs(30 * 1000));
        stop = true;

        joinAll(modifyThreads);
        //since half of the transactions are going to be aborted we need to divide it by 2

        assertEquals(sum(modifyThreads), ref.get());
    }

    public long sum(ModifyThread[] threads){
        long result = 0;
        for(ModifyThread thread: threads){
            result+=thread.writeCount;
        }
        return result;
    }

    public class ModifyThread extends TestThread {

        long writeCount;

        public ModifyThread(int id) {
            super("ModifyThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            while(!stop){
                if (writeCount % 500000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), writeCount);
                }

                boolean abort = randomOneOf(10);
                if (abort) {
                    try {
                        modifyButAbort();
                        fail();
                    } catch (DeadTransactionException ignore) {
                    }
                } else {
                   writeCount++;
                   modify();
                }
            }
        }

        @TransactionalMethod
        private void modify() {
            ref.inc();
        }

        @TransactionalMethod
        private void modifyButAbort() {
            ref.inc();
            getThreadLocalTransaction().abort();
        }
    }
}
