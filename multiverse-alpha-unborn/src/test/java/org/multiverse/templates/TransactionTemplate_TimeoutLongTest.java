package org.multiverse.templates;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.exceptions.RetryTimeoutException;
import org.multiverse.transactional.DefaultTransactionalReference;
import org.multiverse.transactional.TransactionalReference;
import org.multiverse.transactional.primitives.TransactionalInteger;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.sleepMs;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionTemplate_TimeoutLongTest {
    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void whenTimeout() {
        final TransactionalReference ref = new DefaultTransactionalReference();

        TransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setTimeoutNs(TimeUnit.SECONDS.toNanos(1))
                .build();

        TransactionTemplate t = new TransactionTemplate(txFactory) {
            @Override
            public Object execute(Transaction tx) throws Exception {
                System.out.println(ref.get());
                if (ref.isNull()) {
                    retry();
                }

                return null;
            }
        };

        try {
            t.execute();
            fail();
        } catch (RetryTimeoutException expected) {
        }
    }

    @Test
    public void whenSomeWaitingNeeded() {
        final TransactionalInteger ref = new TransactionalInteger();

        TransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setTimeoutNs(TimeUnit.SECONDS.toNanos(10))
                .build();

        TransactionTemplate t = new TransactionTemplate(txFactory) {
            @Override
            public Object execute(Transaction tx) throws Exception {
                System.out.println("ref.value=" + ref.get());
                if (ref.get() < 50) {
                    retry();
                }

                return null;
            }
        };

        TestThread notifyThread = new TestThread() {
            @Override
            public void doRun() throws Exception {
                for (int k = 0; k < 100; k++) {
                    ref.inc();
                    sleepMs(100);
                }
            }
        };
        notifyThread.start();

        t.execute();

        joinAll(notifyThread);
    }

    @Test
    public void multipleWakeupsButNotEnough() {
        final TransactionalInteger ref = new TransactionalInteger();

        TransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setTimeoutNs(TimeUnit.SECONDS.toNanos(5))
                .build();

        TransactionTemplate t = new TransactionTemplate(txFactory) {
            @Override
            public Object execute(Transaction tx) throws Exception {
                System.out.println("ref.value=" + ref.get());
                if (ref.get() < 10000) {
                    retry();
                }

                return null;
            }
        };

        TestThread notifyThread = new TestThread() {
            @Override
            public void doRun() throws Exception {
                for (int k = 0; k < 100; k++) {
                    ref.inc();
                    sleepMs(100);
                }
            }
        };
        notifyThread.start();

        try {
            t.execute();
            fail();
        } catch (RetryTimeoutException expected) {
        }

        joinAll(notifyThread);
    }
}
