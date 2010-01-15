package org.multiverse.integrationtests.classicproblems;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.transactional.primitives.TransactionalInteger;

import static org.multiverse.TestUtils.testIncomplete;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * This implementation is a lot easier than the solution provided on the wiki page. Don't see the
 * use for the arbiter.
 * <p/>
 * <p/>
 * For more information see:
 * http://en.wikipedia.org/wiki/Cigarette_smokers_problem
 * <p/>
 *
 * @author Peter Veentjer.
 */
public class CigaretteSmokersProblemSimplifiedTest {
    private TransactionalInteger tobacco;
    private TransactionalInteger paper;
    private TransactionalInteger match;
    private int cigarettesToSmoke = 10 * 1000 * 1000;

    @Before
    public void setUp() {
        setThreadLocalTransaction(null);
        tobacco = new TransactionalInteger(0);
        paper = new TransactionalInteger(0);
        match = new TransactionalInteger(0);
    }

    @After
    public void tearDown() {
        //    stm.getProfiler().print();
    }

    @Test
    public void test() {
        SmokerThread tobaccoDude = new SmokerThread("tobaccoDude", tobacco);
        SmokerThread paperDude = new SmokerThread("paperDude", paper);
        SmokerThread matchDude = new SmokerThread("matchDude", match);

        //startAll(tabaccoDude, paperDude, matchDude);
        //joinAll(tabaccoDude, paperDude, matchDude);
        testIncomplete();
    }

    class SmokerThread extends TestThread {
        private TransactionalInteger resource;

        public SmokerThread(String name, TransactionalInteger resource) {
            super(name);

            this.resource = resource;
        }

        @Override
        public void doRun() {
            for (int k = 0; k < cigarettesToSmoke; k++) {
                giveResource();
                smokeCigarette();

                if (k % (1 * 1000) == 0) {
                    System.out.printf("%s did work on %s cigarettes\n", getName(), k);
                }
            }
        }

        @TransactionalMethod
        private void giveResource() {
            if (resource.get() == 0) {
                resource.inc();
            }
        }

        @TransactionalMethod
        private void smokeCigarette() {
            if (tobacco.get() == 1 && paper.get() == 1 && match.get() == 1) {
                tobacco.dec();
                paper.dec();
                match.dec();
                resource.inc();
            } else {
                resource.inc();//probleem is dat deze dus rolled back wordt.
                retry();
            }
        }
    }
}
