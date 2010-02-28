package org.multiverse.integrationtests.isolation;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.transactional.primitives.TransactionalInteger;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.String.format;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.multiverse.TestUtils.*;

public class AnotherWriteSkewStressTest {

    private int transactionCountPerThread = 10 * 1000 * 1000;
    private User user1;
    private User user2;
    private AtomicBoolean writeSkewEncountered = new AtomicBoolean();
    private boolean allowWriteSkewProblem;
    private TransferThread[] threads;

    @Before
    public void setUp() {
        user1 = new User();
        user2 = new User();
        writeSkewEncountered.set(false);

        threads = new TransferThread[2];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new TransferThread(k);
        }

        user1.getRandomAccount().inc(1000);
    }

    @Test
    public void writeSkewAllowed() {
        allowWriteSkewProblem = true;
        startAll(threads);
        joinAll(threads);

        System.out.println("User1: " + user1);
        System.out.println("User2: " + user2);

        assertTrue(writeSkewEncountered.get());
    }

    @Test
    public void writeSkewProblemNotAllowed() {
        allowWriteSkewProblem = false;
        startAll(threads);
        joinAll(threads);

        System.out.println("User1: " + user1);
        System.out.println("User2: " + user2);

        assertFalse("writeskew detected", writeSkewEncountered.get());
    }


    public class TransferThread extends TestThread {

        public TransferThread(int id) {
            super("TransferThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            for (int k = 0; k < transactionCountPerThread; k++) {
                if (k % 1000000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }

                if (allowWriteSkewProblem) {
                    runWithPreventWriteSkew();
                } else {
                    runWithoutPreventWriteSkew();
                }
            }
        }

        @TransactionalMethod(automaticReadTracking = false)
        private void runWithoutPreventWriteSkew() {
            doIt();
        }

        @TransactionalMethod(automaticReadTracking = true, allowWriteSkewProblem = true)
        private void runWithPreventWriteSkew() {
            doIt();
        }

        public void doIt() {
            int amount = randomInt(100);

            User from = random(user1, user2);
            User to = random(user1, user2);

            int sum = from.account1.get() + from.account2.get();

            if (sum < 0) {
                writeSkewEncountered.set(true);
                System.out.println("writeskew detected");
            }

            if (sum >= amount) {
                from.getRandomAccount().dec(amount);
                to.getRandomAccount().inc(amount);
                //sleepUs(100);
            }
        }
    }

    public User random(User user1, User user2) {
        return randomBoolean() ? user1 : user2;
    }

    public class User {

        private TransactionalInteger account1 = new TransactionalInteger();
        private TransactionalInteger account2 = new TransactionalInteger();

        public TransactionalInteger getRandomAccount() {
            return randomBoolean() ? account1 : account2;
        }

        @TransactionalMethod(readonly = true)
        public int getTotal() {
            return account1.get() + account2.get();
        }

        public String toString() {
            return format("User(account1 = %s, account2 = %s)", account1.get(), account2.get());
        }
    }
}
