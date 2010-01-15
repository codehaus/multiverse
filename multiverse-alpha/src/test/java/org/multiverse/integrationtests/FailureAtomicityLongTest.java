package org.multiverse.integrationtests;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.ThreadLocalTransaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.transactional.primitives.TransactionalInteger;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;

/**
 * The FailureAtomicityLongTest tests if failure atomicity works. So it should be impossible that the eventual
 * state is based on transactions that are eventually rolled back.
 * <p/>
 * There is a shared TransactionalInteger that is concurrently increased. Half of the transactions is going to be aborted
 * so with a writecount of x and a modifyThreadCount of y, the TransactionalInteger.get() will be x*y/2.
 *
 * @author Peter Veentjer.
 */
public class FailureAtomicityLongTest {

    private int modifyThreadCount = 10;
    private int writeCount = 1000 * 1000;
    private TransactionalInteger ref;

    @Before
    public void setUp() {
        ref = new TransactionalInteger();
    }

    @Test
    public void test() {
        ModifyThread[] modifyThreads = new ModifyThread[modifyThreadCount];
        for (int k = 0; k < modifyThreadCount; k++) {
            modifyThreads[k] = new ModifyThread(k);
        }

        startAll(modifyThreads);
        joinAll(modifyThreads);
        //since half of the transactions are going to be aborted we need to divide it by 2
        int expected = (modifyThreadCount * writeCount) / 2;
        assertEquals(expected, ref.get());
    }

    public class ModifyThread extends TestThread {
        public ModifyThread(int id) {
            super("ModifyThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            for (int k = 0; k < writeCount; k++) {
                if (k % 50000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }

                boolean abort = k % 2 == 0;
                if (abort) {
                    try {
                        modifyButAbort();
                        fail();
                    } catch (DeadTransactionException ignore) {
                    }
                } else {
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
            ThreadLocalTransaction.getThreadLocalTransaction().abort();
        }
    }
}
