package org.multiverse.templates;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.transactional.refs.IntRef;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * A test that makes sure that the TransactionTemplate behaves well with a lot of read and write conflicts.
 * <p/>
 * The problem:
 * There is an array with TransactionalIntegers, and each transaction wants to increase each element in that
 * array. This will cause a lot of read and writeconflicts since they are working on a very concurrently used
 * read and writeset.
 *
 * @author Peter Veentjer
 */
public class TransactionTemplate_conflictStressTest {

    private int threadCount = 4;
    private int transactionsPerThread = 500 * 1000;
    private int refCount = 40;

    private IntRef[] refs;
    private Stm stm;
    private IncThread[] threads;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();

        refs = new IntRef[refCount];
        for (int k = 0; k < refCount; k++) {
            refs[k] = new IntRef();
        }

        threads = new IncThread[threadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new IncThread(k);
        }
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    private int sum() {
        int sum = 0;
        for (IntRef ref : refs) {
            sum += ref.get();
        }
        return sum;
    }

    @Test
    public void test() {
        startAll(threads);
        joinAll(threads);

        assertEquals(refs.length * threadCount * transactionsPerThread, sum());
    }


    public class IncThread extends TestThread {

        public IncThread(int id) {
            super("IncThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            for (int k = 0; k < transactionsPerThread; k++) {
                if (k % 10000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }

                action();
            }
        }

        public void action() {
            TransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                    .setReadonly(false)
                    .setFamilyName(getClass() + "action()")
                    .setReadTrackingEnabled(false).build();

            new TransactionTemplate(txFactory) {
                @Override
                public Object execute(Transaction tx) throws Exception {
                    //sleepUs(100);

                    for (int k = 0; k < refCount; k++) {
                        refs[k].inc();
                    }
                    return null;
                }
            }.execute();
        }
    }
}
