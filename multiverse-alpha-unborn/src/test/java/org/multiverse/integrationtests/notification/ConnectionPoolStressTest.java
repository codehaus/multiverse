package org.multiverse.integrationtests.notification;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.transactional.collections.TransactionalLinkedList;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;

/**
 * A test that tests if a connection pool can be made using stm.
 *
 * @author Peter Veentjer.
 */
public class ConnectionPoolStressTest {

    private int poolsize = 8;
    private int threadCount = 10;
    private int useCount = 1000;

    private ConnectionPool pool;

    @Before
    public void setUp() {
        pool = new ConnectionPool(poolsize);
    }

    @Test
    public void test() {
        WorkerThread[] threads = createThreads();
        startAll(threads);
        joinAll(threads);

        assertEquals(poolsize, pool.size());
    }

    @TransactionalObject
    static class ConnectionPool {

        final TransactionalLinkedList<Connection> deque = new TransactionalLinkedList<Connection>();

        ConnectionPool(int poolsize) {
            fill(poolsize);
        }

        private void fill(int poolsize) {
            for (int k = 0; k < poolsize; k++) {
                deque.add(new Connection());
            }
        }

        @TransactionalMethod(maxRetries = 10000)
        Connection takeConnection() throws InterruptedException {
            return deque.takeFirst();
        }

        void returnConnection(Connection c) {
            try {
                deque.putLast(c);
            } catch (InterruptedException e) {
                fail();
            }
        }

        int size() {
            return deque.size();
        }
    }

    //the methods are there to make sure that concurrent usage of a connection is detected (and fails the test).

    static class Connection {

        AtomicInteger users = new AtomicInteger();

        void startUsing() {
            if (!users.compareAndSet(0, 1)) {
                fail();
            }
        }

        void stopUsing() {
            if (!users.compareAndSet(1, 0)) {
                fail();
            }
        }
    }

    private WorkerThread[] createThreads() {
        WorkerThread[] threads = new WorkerThread[threadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new WorkerThread(k);
        }
        return threads;
    }

    class WorkerThread extends TestThread {

        public WorkerThread(int id) {
            super("WorkerThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            for (int k = 0; k < useCount; k++) {
                if (k % 100 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }

                Connection c = pool.takeConnection();
                assertNotNull(c);
                c.startUsing();

                try {
                    sleepRandomMs(50);
                } finally {
                    c.stopUsing();
                    pool.returnConnection(c);
                }
            }
        }
    }
}
