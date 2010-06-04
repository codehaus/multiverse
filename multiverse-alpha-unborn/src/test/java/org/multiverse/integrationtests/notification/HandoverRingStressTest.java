package org.multiverse.integrationtests.notification;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.integrationtests.Ref;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * A ring of threads that keep passing each other a new event.. so one after another.
 * One threads waits on the left side for a value to become 1. Once this happens, the left
 * value is set to 0, and a value on the right side is set to 1. So you have a ring of threads
 * that handover a token.
 *
 * @author Peter Veentjer
 */
public class HandoverRingStressTest {

    private int handoverCount = 50 * 1000;
    private int threadCount = 10;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test
    public void test() {
        Ref[] refs = new Ref[threadCount];
        for (int k = 0; k < refs.length; k++) {
            refs[k] = new Ref();
        }

        HandoverThread[] threads = new HandoverThread[threadCount];
        for (int k = 0; k < refs.length; k++) {
            Ref left = refs[k];
            Ref right = k == (refs.length - 1) ? refs[0] : refs[k + 1];
            threads[k] = new HandoverThread(k, left, right);
        }

        startAll(threads);
        //feed initial token
        refs[0].set(1);
        joinAll(threads);

        for (HandoverThread t : threads) {
            assertEquals(handoverCount, t.handovers);
        }
    }

    class HandoverThread extends TestThread {

        private final Ref left;
        private final Ref right;
        private int handovers;
        private final int id;

        HandoverThread(int id, Ref left, Ref right) {
            super("HandoverThread-" + id);
            this.left = left;
            this.right = right;
            this.id = id;
        }

        @Override
        public void doRun() throws Exception {
            for (int k = 0; k < handoverCount; k++) {

                left.await(1);

                left.set(0);
                right.set(1);

                handovers++;

                if (k % 50000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }
        }
    }
}
