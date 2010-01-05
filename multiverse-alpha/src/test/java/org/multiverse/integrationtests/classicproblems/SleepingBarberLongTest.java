package org.multiverse.integrationtests.classicproblems;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

import org.multiverse.transactional.DefaultTransactionalReference;
import org.multiverse.transactional.annotations.TransactionalMethod;
import org.multiverse.transactional.collections.TransactionalLinkedList;
import org.multiverse.transactional.primitives.TransactionalInteger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The Sleeping Barber problem, due to legendary Dutch computer scientist <a href="http://en.wikipedia.org/wiki/Edsger_Dijkstra">E.W.
 * Dijkstra</a>, is stated on <a href="http://en.wikipedia.org/wiki/Sleeping_barber">Wikipedia</a>) as follows:
 * <p/>
 * &quot;A hypothetical barber shop with one barber, one barber chair, and a number of chairs for waiting customers.
 * When there are no customers, the barber sits in his chair and sleeps. As soon as a customer arrives, he either
 * awakens the barber or, if the barber is cutting someone else's hair, sits down in one of the vacant chairs. If all of
 * the chairs are occupied, the newly arrived customer simply leaves.&quot;
 * <p/>
 * The problem is trivially modelled using a fixed-size {@linkplain BlockingQueue blocking queue} to represent the
 * chairs, with customers adding themselves to the queue and the barber doing a series of {@link BlockingQueue#take()
 * takes}:
 * <p/>
 * <pre>
 * BlockingQueue chairs = new BlockingQueue(chairCount);
 * <p/>
 * class Customer {
 *   void visit() {
 *     if (!chairs.offer(this)) {
 *       // all chairs occupied: leave
 *     } else {
 *       // waiting for haircut
 *     }
 *   }
 * }
 * <p/>
 * class Barber {
 *   void runShop() {
 *     while (!closingTime) {
 *       cutHair(chairs.take());
 *     }
 *   }
 * }
 * </pre>
 * <p/>
 * This is very close to the algorithm given in Dijkstra's <a href="http://www.cs.utexas.edu/users/EWD/transcriptions/EWD01xx/EWD123.html#4.2.%20The%20Superfluity%20of%20the%20General%20Semaphore.">original
 * paper</a>, which is a producer/consumer example in which the consumer waits when the queue is empty and consumers
 * notify the producer when they add an element to an <em>empty</em> queue.
 * <p/>
 * But using a blocking queue would be &quot;cheating&quot;, sort-of, because the tricky part of the problem - getting
 * the handover between the customers and barber right - is encapsulated in the queue.
 * <p/>
 * Also, the model is slightly odd: the barber sleeps standing up or, at any rate, not in one of the chairs, a customer
 * sitting down in a chair magically wakes the barber, the barber &quot;pulls&quot; customers out of their chairs etc.
 * <p/>
 * So, instead, we'll here implement the &quot;canonical&quot; solution given in the Wikipedia article, which uses two
 * flags to coordinate handover. The third semaphore of the canonical solution is replaced by transactions, which after
 * all is the point of this example.
 *
 * @author Andrew Phillips
 */
public class SleepingBarberLongTest {

    // the chairs are used by the customers *and* the barber
    private final int chairCount = 5;
    private BlockingQueue<TestThread> chairs;

    BarberThread barber;

    // each "customer thread" simulates the visits of a succession of customers 
    private final int customerThreadCount = 5;
    private final int customerCount = 100;

    private final AtomicInteger customerCountDown = new AtomicInteger();
    private final AtomicInteger customersTurnedAway = new AtomicInteger();
    private final AtomicLong customersServed = new AtomicLong();

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        chairs = new TransactionalLinkedList<TestThread>(chairCount);
    }

    @After
    public void tearDown() {
//        ProfileRepository profiler = 
//            ((AlphaStm) GlobalStmInstance.getGlobalStmInstance()).getProfiler();
//        new ProfilePublisher(profiler.getCollator()).writeStatisticsToStream(System.out);
    }

    @Test
    public void test() {
        barber = new BarberThread();
        customerCountDown.set(customerCount);
        CustomerThread[] customers = createCustomerThreads();

        startAll(barber);
        startAll(customers);

        joinAll(customers);
        barber.closeShop();
        joinAll(barber);

        assertTrue("Expected the shop to be empty", chairs.isEmpty());
        assertEquals(customerCount, customersServed.get() + customersTurnedAway.get());
    }

    private CustomerThread[] createCustomerThreads() {
        CustomerThread[] threads = new CustomerThread[customerThreadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new CustomerThread(k);
        }
        return threads;
    }

    public class BarberThread extends TestThread {

        private DefaultTransactionalReference<Boolean> closingTime = new DefaultTransactionalReference<Boolean>();

        // 0 == asleep, 1 == awake
        private TransactionalInteger state = new TransactionalInteger();

        BarberThread() {
            super("BarberThread");
        }

        @Override
        public void doRun() {
            closingTime.set(false);
            state.set(1);

            while (true) {
                fallAsleepIfShopEmpty();
                snoozeUntilWoken();

                if (isClosingTime()) {
                    break;
                }

                /*
                * chairs.element() followed by customer.askForward() is just a long
                * drawn-out version of chairs.remove().
                * From a modelling perspective, the idea is that the barber doesn't
                * pull the customer out of his/her chair (unless you know some very
                * no-nonsense barbers!), but asks one of the seated customers, who
                * then gets up him- or herself.
                */
                CustomerThread customer = (CustomerThread) chairs.element();
                customer.askForward();
                cutHair(customer);
                customer.showOut();
                customersServed.incrementAndGet();
            }
        }

        /*
         * CRITICAL SECTION: There must not be a pause between the barber realising the
         * shop is empty and falling asleep. Otherwise, a customer could enter the shop
         * before the barber fell asleep, but not bother waking him (because he is not
         * asleep yet!).
         * We would end up with a sleeping barber and a waiting customer: deadlock. 
         */
        @TransactionalMethod
        private void fallAsleepIfShopEmpty() {
            if (chairs.isEmpty() && !isClosingTime()) {
                // the barber sits down in a chair and falls asleep
                chairs.add(this);
                state.set(0);
            }
        }

        private void snoozeUntilWoken() {
            //todo:
            if (isAsleep()) {
                /*
                 * This has to take place outside the "takeNapIfShopEmpty" atomic block
                 * because we need that block to be committed before the barber waits to
                 * be woken up.
                 */
                state.await(1);

                // the barber gets up when he's awoken
                boolean wasSeated = chairs.remove(this);
                assert wasSeated : "The barber wasn't seated?!?";
            }
        }

        private void cutHair(CustomerThread customer) {
            // some people have long hair, some have short hair - all takes time
            sleepRandomMs(50);
        }

        public void wakeUp() {
            state.inc();
        }

        public boolean isAsleep() {
            return (state.get() == 0);
        }

        public boolean isAwake() {
            return (state.get() == 1);
        }

        private boolean isClosingTime() {
            return closingTime.get();
        }

        public void closeShop() {
            closingTime.set(true);

            // the barber instinctively wakes up at closing time if asleep 
            state.set(1);
        }
    }

    public class CustomerThread extends TestThread {

        // 0 == sat down, 1 == beckoned by barber, 2 == got up for haircut, 3 == left shop
        private final TransactionalInteger state = new TransactionalInteger();

        CustomerThread(int id) {
            super("CustomerThread-" + id);
        }

        @Override
        public void doRun() {
            while (customerCountDown.decrementAndGet() >= 0) {
                // wait a certain amount of time before sending each customer
                sleepRandomMs(250);
                visitBarber();

                // wait until the customer has left the shop
                state.await(3);
            }
        }

        private void visitBarber() {

            if (!tryToSitDown()) {
                leaveShop();
                customersTurnedAway.incrementAndGet();
                return;
            }

            wakeBarberIfAsleep();
            assert barber.isAwake() : "The barber wasn't woken up or was woken by too many customers!";
            waitForTurn();
        }

        /*
        * CRITICAL SECTION: There may not be a pause between the customer sitting down
        * and setting his state to "waiting". Otherwise, s/he may be beckoned forward
        * by the barber before s/he is waiting.
        */
        @TransactionalMethod
        private boolean tryToSitDown() {
            if (!chairs.offer(this)) {
                return false;
            } else {
                state.set(0);
                return true;
            }
        }

        private int leaveShop() {
            return state.set(3);
        }

        /*
         * CRITICAL SECTION: There must not be a pause between a customer deciding to wake
         * the barber and the barber waking up. Otherwise, a second customer could also
         * decide to wake the barber, and both would try to wake him.
         * Depending on the interpretation, this isn't really a problem (it's just equivalent 
         * to a superfluous notification sent to the consumer), but it's slightly more
         * interesting to try to prevent it.
         */
        @TransactionalMethod
        private void wakeBarberIfAsleep() {
            if (barber.isAsleep()) {
                barber.wakeUp();
            }
        }

        private void waitForTurn() {
            state.await(1);

            boolean wasSeated = chairs.remove(this);
            assert wasSeated : "Customer wasn't seated?!?";

            // walk over to the barber
            int wasCalled = state.set(2);
            assert (wasCalled == 1) : String.format("Customer %s stood up but wasn't called (state: %d)?!?",
                                                    getName(),
                                                    wasCalled);
        }

        public void askForward() {
            int wasWaiting = state.set(1);
            assert (wasWaiting == 0) : String.format(
                    "The barber signalled a customer (%s) who wasn't waiting (state: %d)?!?",
                    getName(),
                    wasWaiting);

            state.await(2);
        }

        public void showOut() {
            int wasHavingHairCut = leaveShop();
            assert (wasHavingHairCut == 2) : String.format(
                    "Customer %s was shown out without having his/her hair cut?!?",
                    getName());
        }
    }
}