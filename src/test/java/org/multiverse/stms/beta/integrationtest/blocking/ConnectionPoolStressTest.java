package org.multiverse.stms.beta.integrationtest.blocking;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicClosure;
import org.multiverse.api.closures.AtomicIntClosure;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.BetaStmUtils;
import org.multiverse.stms.beta.transactionalobjects.BetaIntRef;
import org.multiverse.stms.beta.transactionalobjects.BetaRef;
import org.multiverse.stms.beta.transactionalobjects.IntRefTranlocal;
import org.multiverse.stms.beta.transactionalobjects.RefTranlocal;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmTestUtils.newIntRef;
import static org.multiverse.stms.beta.BetaStmTestUtils.newRef;

/**
 * A StressTest that simulates a database connection pool. The code is quite ugly, but that is because
 * no instrumentation is used here.
 *
 * @author Peter Veentjer.
 */
public class ConnectionPoolStressTest implements BetaStmConstants {
    private int poolsize = processorCount();
    private int threadCount = processorCount() * 2;
    private volatile boolean stop;

    private ConnectionPool pool;
    private BetaStm stm;
    private int lockMode;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = new BetaStm();
        pool = new ConnectionPool(poolsize);
        stop = false;
    }

    @Test
    public void testEnsured() {
        test(LOCKMODE_UPDATE);
    }

    @Test
    public void testPrivatized() {
        test(LOCKMODE_COMMIT);
    }

    @Test
    public void testNoLocking() {
        test(LOCKMODE_NONE);
    }

    public void test(int lockMode) {
        this.lockMode = lockMode;
        WorkerThread[] threads = createThreads();

        startAll(threads);

        sleepMs(30 * 1000);
        stop = true;

        joinAll(threads);
        assertEquals(poolsize, pool.size());
    }

    class ConnectionPool {
        final AtomicBlock takeConnectionBlock = stm.createTransactionFactoryBuilder()
                .setMaxRetries(10000)
                .buildAtomicBlock();

        final AtomicBlock returnConnectionBlock = stm.createTransactionFactoryBuilder()
                .buildAtomicBlock();

        final AtomicBlock sizeBlock = stm.createTransactionFactoryBuilder().buildAtomicBlock();

        final BetaIntRef size = newIntRef(stm);
        final BetaRef<Node<Connection>> head = newRef(stm);

        ConnectionPool(final int poolsize) {
            stm.getDefaultAtomicBlock().execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;

                    RefTranlocal<Node<Connection>> headTranlocal = btx.openForWrite(head, lockMode);

                    for (int k = 0; k < poolsize; k++) {
                        headTranlocal.value = new Node(headTranlocal.value, new Connection());
                        btx.openForWrite(size, lockMode).value++;
                    }
                }
            });
        }

        Connection takeConnection() {
            return takeConnectionBlock.execute(new AtomicClosure<Connection>() {
                @Override
                public Connection execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;

                    IntRefTranlocal sizeTranlocal = btx.openForWrite(size, lockMode);
                    if (sizeTranlocal.value == 0) {
                        retry();
                    }

                    sizeTranlocal.value--;

                    RefTranlocal<Node<Connection>> headTranlocal = btx.openForWrite(head, lockMode);
                    Node<Connection> oldHead = headTranlocal.value;
                    headTranlocal.value = oldHead.next;
                    return oldHead.item;
                }
            });
        }

        void returnConnection(final Connection c) {
            returnConnectionBlock.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;

                    btx.openForWrite(size, lockMode).value++;

                    RefTranlocal<Node<Connection>> headTranlocal = btx.openForWrite(head, lockMode);

                    Node<Connection> oldHead = headTranlocal.value;
                    headTranlocal.value = new Node<Connection>(oldHead, c);
                }
            });
        }

        int size() {
            return sizeBlock.execute(new AtomicIntClosure() {
                @Override
                public int execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    return btx.openForRead(size, LOCKMODE_NONE).value;
                }
            });
        }
    }

    static class Node<E> {
        final Node<E> next;
        final E item;

        Node(Node<E> next, E item) {
            this.next = next;
            this.item = item;
        }
    }

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
            int k = 0;
            while (!stop) {
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
                k++;
            }
        }
    }
}
