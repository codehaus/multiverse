package org.multiverse.integrationtests.isolation;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.Transaction;
import org.multiverse.transactional.primitives.TransactionalInteger;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

/**
 * A Test that checks when writeSkew detection has been disabled and can happen, and with writeskewdetection enabled
 * that it can't happen.
 *
 * @author Peter Veentjer.
 */
public class WriteSkewStressTest {

    private AtomicBoolean writeSkewOccurred = new AtomicBoolean();
    private int bankCustomerCount = 10;
    private int threadCount = 20;
    private int txCountPerThread = 1000;

    private TransactionalInteger[] accounts;
    private TransferThread[] threads;

    @Before
    public void setUp() {
        writeSkewOccurred.set(false);
        clearThreadLocalTransaction();

        accounts = new TransactionalInteger[bankCustomerCount * 2];
        for (int k = 0; k < accounts.length; k++) {
            accounts[k] = new TransactionalInteger();
        }

        threads = new TransferThread[threadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new TransferThread(k);
        }
    }

    @Test
    public void longTestWithWriteSkewDisabledEventuallyGivesWriteSkewProblems() {
        for (int k = 0; k < accounts.length; k++) {
            //accounts[k].set(randomInt(1000));
            accounts[k].set(50);
        }

        printAccounts();

        int initialSum = sum();

        startAll(threads);
        joinAll(threads);

        //this condition should fail
        assertTrue(writeSkewOccurred.get());

        printAccounts();

        assertEquals(initialSum, sum());
    }

    private void printAccounts() {
        for (int k = 0; k < accounts.length; k++) {
            System.out.println("k " + k + " " + accounts[k].get());
        }
    }

    public int sum() {
        int sum = 0;
        for (int k = 0; k < accounts.length; k++) {
            sum += accounts[k].get();
        }

        return sum;
    }

    public class TransferThread extends TestThread {

        public TransferThread(int id) {
            super("TransferThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            for (int k = 0; k < txCountPerThread; k++) {
                if (k % 1000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }

                int fromUser = randomInt(bankCustomerCount);
                int toUser = randomInt(bankCustomerCount);

                if (fromUser != toUser) {
                    transfer(fromUser, toUser, 10);
                }
            }
        }

        @TransactionalMethod(automaticReadTracking = true, preventWriteSkew = false)
        public void transfer(int fromUser, int toUser, int amount) {

            TransactionalInteger fromAccount1 = accounts[fromUser * 2];
            TransactionalInteger fromAccount2 = accounts[fromUser * 2 + 1];

            //here do the open for read/write take place.
            int sum = fromAccount1.get() + fromAccount2.get();
            if (sum < 0) {
                System.out.println("WriteSkew occurred");
                writeSkewOccurred.set(true);
            }

            if (sum >= amount) {
                if (randomBoolean()) {
                    fromAccount1.dec(amount);
                } else {
                    fromAccount2.dec(amount);
                }

                if (randomBoolean()) {
                    accounts[toUser * 2].inc(amount);
                } else {
                    accounts[toUser * 2 + 1].inc(amount);
                }
            }

            //by delaying here, we enlarge the period of 2 transactions opening in the violating state.
            //we we increase the change if finding problems
            sleepMs(1);
        }
    }

    @Test
    @Ignore
    public void simpleTestWithWriteSkewDetectionDisabledRepeated() {
        for (int k = 0; k < 100; k++) {
            simpleTestWithWriteSkewDisabled();
        }
    }

    public void simpleTestWithWriteSkewDisabled() {
        TransactionalInteger from1 = new TransactionalInteger(100);
        TransactionalInteger from2 = new TransactionalInteger(0);

        TransactionalInteger to1 = new TransactionalInteger();
        TransactionalInteger to2 = new TransactionalInteger();

        NonDetectingThread t1 = new NonDetectingThread(from1, from2, to1);
        NonDetectingThread t2 = new NonDetectingThread(from2, from1, to2);

        //there is a big fat delay before the tx completes, so would but both threads in the problem area.
        t1.start();
        t2.start();

        joinAll(t1, t2);

        assertTrue(from1.get() + from2.get() < 0);

        System.out.printf("from1=%s from2=%s  total=%s\n", from1.get(), from2.get(), from1.get() + from2.get());
        System.out.printf("to1=%s to1=%s  total=%s\n", to1.get(), to2.get(), to1.get() + to2.get());
    }

    private class NonDetectingThread extends TestThread {

        final TransactionalInteger from1;
        final TransactionalInteger from2;
        final TransactionalInteger to;

        private NonDetectingThread(TransactionalInteger from1, TransactionalInteger from2, TransactionalInteger to) {
            this.from1 = from1;
            this.from2 = from2;
            this.to = to;
        }

        @Override
        @TransactionalMethod(preventWriteSkew = false)
        public void doRun() throws Exception {
            if (from1.get() + from2.get() >= 100) {
                from1.dec(100);
                to.inc(100);
            }
            sleepMs(500);
        }
    }

    @Test
    public void simpleTestWithWriteSkewDetectionEnabledRepeated() {
        for (int k = 0; k < 100; k++) {
            simpleTestWithWriteSkewDetectionEnabled();
        }
    }

    public void simpleTestWithWriteSkewDetectionEnabled() {
        TransactionalInteger from1 = new TransactionalInteger(100);
        TransactionalInteger from2 = new TransactionalInteger(0);

        TransactionalInteger to1 = new TransactionalInteger(0);
        TransactionalInteger to2 = new TransactionalInteger(0);

        DetectingThread t1 = new DetectingThread(from1, from2, to1);
        DetectingThread t2 = new DetectingThread(from2, from1, to2);

        t2.start();
        t1.start();

        joinAll(t2);
        joinAll(t1);

        assertTrue(from1.get() + from2.get() >= 0);
    }

    private class DetectingThread extends TestThread {

        final TransactionalInteger from1;
        final TransactionalInteger from2;
        final TransactionalInteger to;

        private DetectingThread(TransactionalInteger from1, TransactionalInteger from2, TransactionalInteger to) {
            this.from1 = from1;
            this.from2 = from2;
            this.to = to;
        }

        @Override
        @TransactionalMethod(preventWriteSkew = true)
        public void doRun() throws Exception {
            if (from1.get() + from2.get() >= 100) {
                from1.dec(100);
                to.inc(100);
                //System.out.println("enough cash");
            } else {
                //System.out.println("not enough cash");
            }

            Transaction tx = getThreadLocalTransaction();
            assertFalse(tx.getConfig().isReadonly());
            assertTrue(tx.getConfig().automaticReadTracking());
            assertTrue(tx.getConfig().preventWriteSkew());

            sleepMs(200);
        }
    }
}
