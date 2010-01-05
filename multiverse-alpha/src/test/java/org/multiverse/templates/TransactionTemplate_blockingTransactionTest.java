package org.multiverse.templates;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.Stm;
import org.multiverse.transactional.primitives.TransactionalInteger;

import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

public class TransactionTemplate_blockingTransactionTest {
    private Stm stm;
    private TransactionalInteger ref;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        ref = new TransactionalInteger(0);
    }

    @Test
    public void test() {
        WaiterThread thread1 = new WaiterThread(0);
        WaiterThread thread2 = new WaiterThread(1);
        WaiterThread thread3 = new WaiterThread(2);

        startAll(thread1, thread2, thread3);
        joinAll(thread1);

        assertAlive(thread2,thread3);
        ref.set(1);

        joinAll(thread2);
        assertAlive(thread3);

        ref.set(2);
        joinAll(thread3);
    }

    class WaiterThread extends TestThread {
        private final int number;


        public WaiterThread(int number) {
            super("WaiterThread-" + number);
            this.number = number;
        }

        @Override
        public void doRun() throws Exception {
            ref.await(number);
        }
    }
}
