package org.multiverse.integrationtests.traditional;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.TestUtils;
import org.multiverse.annotations.TransactionalObject;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * A integration test to see if a mutex can be created. The mutex itself isn't very useful, but
 * it is a good integration test.
 *
 * @author Peter Veentjer
 */
public class MutexTest {

    private volatile boolean stop;
    private int accountCount = 50;

    private int threadCount = processorCount() * 4;
    private Account[] accounts;


    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stop = false;

    }

    @Test
    public void test() {
        accounts = new Account[accountCount];
        for (int k = 0; k < accountCount; k++) {
            accounts[k] = new Account();
        }

        IncThread[] threads = new IncThread[threadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new IncThread(k);
        }

        startAll(threads);
        sleepMs(TestUtils.getDurationMsFromSystemProperties(60 * 1000));
        stop = true;
        joinAll(threads);

        assertEquals(sum(threads), sum(accounts));
        System.out.println("total increases: "+sum(threads));
    }

    int sum(IncThread[] threads) {
        int result = 0;
        for (IncThread thread : threads) {
            result += thread.count;
        }
        return result;
    }

    int sum(Account[] accounts) {
        int result = 0;
        for (Account account : accounts) {
            result += account.balance;
        }
        return result;
    }

    class IncThread extends TestThread {
        private int count;

        public IncThread(int id) {
            super("IncThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            while (!stop) {
                Account account = accounts[TestUtils.randomInt(accountCount)];
                account.inc();

                if (count % 100000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), count);
                }

                count++;
            }
        }
    }

    class Account {
        final Mutex mutex = new Mutex();

        int balance;

        public void inc() {
            mutex.lock();
            balance++;
            mutex.unlock();
        }
    }

    @TransactionalObject
    class Mutex {
        boolean locked = false;

        public void lock() {
            if (locked) {
                retry();
            }

            locked = true;
        }

        public void unlock() {
            locked = false;
        }
    }
}
