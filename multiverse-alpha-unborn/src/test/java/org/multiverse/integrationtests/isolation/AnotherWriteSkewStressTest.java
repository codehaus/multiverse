package org.multiverse.integrationtests.isolation;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.transactional.refs.IntRef;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.String.format;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class AnotherWriteSkewStressTest {

    private volatile boolean stop;
    private User user1;
    private User user2;
    private boolean allowWriteSkew;
    private TransferThread[] threads;
    private AtomicBoolean writeSkewEncountered = new AtomicBoolean();

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        user1 = new User();
        user2 = new User();
        stop = false;
        writeSkewEncountered.set(false);

        threads = new TransferThread[2];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new TransferThread(k);
        }

        user1.getRandomAccount().inc(1000);
    }

    @Test
    public void allowWriteSkew() {
        allowWriteSkew = true;
        startAll(threads);
        sleepMs(getStressTestDurationMs(60 * 1000));
        stop = true;
        joinAll(threads);

        System.out.println("User1: " + user1);
        System.out.println("User2: " + user2);

        assertTrue(writeSkewEncountered.get());
    }

    @Test
    public void disallowedWriteSkew() {
        allowWriteSkew = false;
        startAll(threads);
        sleepMs(getStressTestDurationMs(60 * 1000));
        stop = true;

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
            int k = 0;
            while (!stop) {
                if (k % 1000000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }

                if (allowWriteSkew) {
                    runWithWriteSkewAllowed();
                } else {
                    runWithWriteSkewDisallowed();
                }
                k++;
            }
        }

        @TransactionalMethod(readonly = false, writeSkew = false)
        private void runWithWriteSkewDisallowed() {
            doIt();
        }

        @TransactionalMethod(readonly = false, writeSkew = true)
        private void runWithWriteSkewAllowed() {
            doIt();
        }

        public void doIt() {
            int amount = randomInt(100);

            User from = random(user1, user2);
            User to = random(user1, user2);

            int sum = from.account1.get() + from.account2.get();

            if (sum < 0) {
                if (!writeSkewEncountered.get()) {
                    writeSkewEncountered.set(true);
                    System.out.println("writeskew detected");
                }
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

        private IntRef account1 = new IntRef();
        private IntRef account2 = new IntRef();

        public IntRef getRandomAccount() {
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
