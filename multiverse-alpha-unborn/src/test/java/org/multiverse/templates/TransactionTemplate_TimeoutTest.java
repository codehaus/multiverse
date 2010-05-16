package org.multiverse.templates;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.exceptions.RetryTimeoutException;
import org.multiverse.api.programmatic.ProgrammaticLong;
import org.multiverse.api.programmatic.ProgrammaticReference;

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
@Ignore
public class TransactionTemplate_TimeoutTest {
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
        System.out.println("----------------whenTimeout------------------");
        final ProgrammaticReference ref = stm.getProgrammaticReferenceFactoryBuilder()
                .build().atomicCreateReference();

        TransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setTimeoutNs(TimeUnit.SECONDS.toNanos(5))
                .build();

        TransactionTemplate template = new TransactionTemplate(txFactory) {
            @Override
            public Object execute(Transaction tx) throws Exception {
                System.out.println("timeout: " + tx.getConfiguration().getTimeoutNs());
                System.out.println("remaining timeout: " + tx.getRemainingTimeoutNs());
                System.out.println("found value=" + ref.get());
                if (ref.isNull()) {
                    retry();
                }

                return null;
            }
        };

        try {
            template.execute();
            fail();
        } catch (RetryTimeoutException expected) {
        }

        System.out.println("---------------------finished whenTimeout------------------");
    }

    @Test
    public void whenSomeWaitingNeeded() {
        System.out.println("whenSomeWaitingNeeded");

        final ProgrammaticLong ref = stm.getProgrammaticReferenceFactoryBuilder()
                .build()
                .atomicCreateLong(0);

        TransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadTrackingEnabled(true)
                .setTimeoutNs(TimeUnit.SECONDS.toNanos(60))
                .build();

        TransactionTemplate template = new TransactionTemplate(txFactory) {
            @Override
            public Object execute(Transaction tx) throws Exception {
                System.out.println("timeout: " + tx.getConfiguration().getTimeoutNs());
                System.out.println("remaining timeout: " + tx.getRemainingTimeoutNs());
                System.out.println("found value=" + ref.get());
                if (ref.get() < 50) {
                    System.out.println("sleeping");
                    retry();
                }

                System.out.println("expected value found");
                return null;
            }
        };

        TestThread notifyThread = new TestThread() {
            @Override
            public void doRun() throws Exception {
                for (int k = 0; k < 100; k++) {
                    System.out.println("incrementing to: " + k);
                    ref.atomicInc(1);
                    System.out.println("finished incrementing to: " + k);
                    sleepMs(100);
                }
            }
        };
        notifyThread.start();

        template.execute();

        joinAll(notifyThread);
    }

    @Test
    public void whenMultipleWakeupsButStillTimeout() {
        System.out.println("multipleWakeupsButNotEnough");

        final ProgrammaticLong ref = stm.getProgrammaticReferenceFactoryBuilder()
                .build()
                .atomicCreateLong(0);

        TransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setReadTrackingEnabled(true)
                .setSpeculativeConfigurationEnabled(false)
                .setTimeoutNs(TimeUnit.SECONDS.toNanos(5))
                .build();

        TransactionTemplate template = new TransactionTemplate(txFactory) {
            @Override
            public Object execute(Transaction tx) throws Exception {
                System.out.println("ref.value=" + ref.get());
                System.out.println("timeout: " + tx.getConfiguration().getTimeoutNs());
                System.out.println("remaining timeout: " + tx.getRemainingTimeoutNs());

                if (ref.get() < 10000) {
                    retry();
                }

                System.out.println("expected value found");
                return null;
            }
        };

        TestThread notifyThread = new TestThread() {
            @Override
            public void doRun() throws Exception {
                for (int k = 0; k < 100; k++) {
                    ref.inc(1);
                    sleepMs(100);
                }
            }
        };
        notifyThread.start();

        try {
            template.execute();
            fail();
        } catch (RetryTimeoutException expected) {
        }

        joinAll(notifyThread);
    }
}
