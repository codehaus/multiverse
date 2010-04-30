package org.multiverse.integrationtests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.FieldGranularity;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.WriteConflict;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

/**
 * A test that makes sure that when fieldgranularity is used instead of the default object granularity/
 */
public class FieldGranularityStressTest {

    public AtomicInteger conflictCounter;
    private int transactionCount = 50 * 1000 * 1000;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        conflictCounter = new AtomicInteger();

    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void testFieldGranularityCausesNoWriteConflicts() {
        Pair pair = new Pair(0, 0);
        SetLeftThread leftThread = new SetLeftThread(pair);
        SetRightThread rightThread = new SetRightThread(pair);

        startAll(leftThread, rightThread);
        joinAll(leftThread, rightThread);

        assertEquals(transactionCount, pair.getLeft());
        assertEquals(transactionCount, pair.getRight());

        assertEquals(0, conflictCounter.get());
    }

    class SetLeftThread extends TestThread {

        final Pair pair;

        SetLeftThread(Pair pair) {
            super("SetLeftThread");
            this.pair = pair;
        }

        @Override
        public void doRun() throws Exception {
            for (int k = 0; k < transactionCount; k++) {
                updateLeft();

                if (k % 5000000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }
        }

        @TransactionalMethod(readonly = false)
        public void updateLeft() {
            pair.setLeft(pair.getLeft() + 1);

            Transaction tx = getThreadLocalTransaction();
            try {
                tx.prepare();
            } catch (WriteConflict conflict) {
                conflict.printStackTrace();
                conflictCounter.incrementAndGet();
            }
        }
    }

    class SetRightThread extends TestThread {

        final Pair pair;

        SetRightThread(Pair pair) {
            super("SetRightThread");
            this.pair = pair;
        }

        @Override
        public void doRun() throws Exception {
            for (int k = 0; k < transactionCount; k++) {
                updateRight();

                if (k % 5000000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }
        }

        @TransactionalMethod(readonly = false)
        public void updateRight() throws Exception {
            pair.setRight(pair.getRight() + 1);
            Transaction tx = getThreadLocalTransaction();

            try {
                tx.prepare();
            } catch (WriteConflict conflict) {
                conflict.printStackTrace();
                conflictCounter.incrementAndGet();
            }
        }
    }

    @TransactionalObject
    public static class Pair {

        @FieldGranularity
        private int left;

        @FieldGranularity
        private int right;

        public Pair(int left, int right) {
            this.left = left;
            this.right = right;
        }

        public int getLeft() {
            return left;
        }

        public void setLeft(int left) {
            this.left = left;
        }

        public int getRight() {
            return right;
        }

        public void setRight(int right) {
            this.right = right;
        }
    }
}
