package org.multiverse.stms.gamma.integration.classic;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.references.BooleanRef;
import org.multiverse.api.references.IntRef;

import static org.multiverse.TestUtils.*;
import static org.multiverse.api.StmUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * http://en.wikipedia.org/wiki/Sleeping_barber_problem
 */
public class SleepingBarberStressTest {

    private BarberShop barberShop;
    private volatile boolean stop;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        barberShop = new BarberShop();
        stop = false;
    }

    @Test
    public void test() {
        BarberThread thread = new BarberThread();
        CustomerSpawnThread spawnThread = new CustomerSpawnThread();

        startAll(thread, spawnThread);
        sleepMs(30 * 1000);
        stop = true;
        joinAll(thread, spawnThread);
    }

    class CustomerSpawnThread extends TestThread {
        public CustomerSpawnThread() {
            super("CustomerSpawnThread");
        }

        @Override
        public void doRun() throws Exception {
            int customerId = 1;
            while (!stop) {
                CustomerThread customerThread = new CustomerThread(customerId);
                customerThread.start();
                customerId++;
                sleepRandomMs(100);
            }
        }
    }

    class BarberThread extends TestThread {
        public BarberThread() {
            super("BarberThread");
        }

        @Override
        public void doRun() {
            AtomicVoidClosure closure = new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    //todo
                }
            };

            while (!stop) {
                execute(closure);
            }

            barberShop.atomicClose();
        }
    }

    class CustomerThread extends TestThread {
        public CustomerThread(int id) {
            super("CustomerThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    if (barberShop.closed.get()) {
                        return;
                    }

                    if (barberShop.freeSeats.get() == 0) {
                        return;
                    }

                    //todo
                }
            });
        }
    }

    class BarberShop {
        private BooleanRef closed = newBooleanRef(false);
        private IntRef freeSeats = newIntRef(5);

        void atomicClose() {
            closed.atomicSet(false);
        }
    }
}
