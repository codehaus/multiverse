package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;

import java.util.concurrent.atomic.AtomicLong;

import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;

/**
 * A stress test that tests concurrent takeFirst and putLasts on a TransactionalLinkedList.
 * <p/>
 * Because a take and put can't happen concurrent in the current TransactionalLinkedList implementation for the moment.
 * This issue already is placed on the issue list.
 * <p/>
 * <p/>
 * The cause of  the problem is that a load is done on an atomic object, that has not been committed. If this happens, a
 * fresh tranlocal is returned, and in this case it has a null as value. That zero causes the problems.
 *
 * @author Peter Veentjer
 */
public class TransactionalLinkedList_TakeFirstPutLastStressTest {

    private int takeFirstThreadCount = 2;
    private int putLastThreadCount = 2;
    private int dequeCapacity;
    private int produceCount = 1000 * 1000;

    private AtomicLong itemGenerator;
    private AtomicLong takeCounter;
    private TransactionalLinkedList<Long> list;
    private PutLastThread[] putLastThreads;
    private TakeFirstThread[] takeFirstThreads;

    @Before
    public void setUp() {
        itemGenerator = new AtomicLong();
        takeCounter = new AtomicLong(putLastThreadCount * produceCount);

        putLastThreads = new PutLastThread[putLastThreadCount];
        for (int k = 0; k < putLastThreadCount; k++) {
            putLastThreads[k] = new PutLastThread(k);
        }

        takeFirstThreads = new TakeFirstThread[takeFirstThreadCount];
        for (int k = 0; k < takeFirstThreadCount; k++) {
            takeFirstThreads[k] = new TakeFirstThread(k);
        }
    }

    @Test
    public void runWithCapacity() {
        dequeCapacity = 10;
        list = new TransactionalLinkedList(dequeCapacity);

        startAll(putLastThreads);
        startAll(takeFirstThreads);
        joinAll(putLastThreads);
        joinAll(takeFirstThreads);
    }

    @Test
    public void runWithoutCapacity() {
        dequeCapacity = Integer.MAX_VALUE;
        list = new TransactionalLinkedList(dequeCapacity);

        startAll(putLastThreads);
        startAll(takeFirstThreads);
        joinAll(putLastThreads);
        joinAll(takeFirstThreads);
    }


    public class TakeFirstThread extends TestThread {

        public TakeFirstThread(int id) {
            super("TakeFirstThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            int k = 0;
            while (takeCounter.decrementAndGet() > 0) {
                Long l = list.takeFirst();
                if (l == null) {
                    throw new NullPointerException();
                }

                if ((k % 100000) == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
                k++;
            }
        }
    }

    public class PutLastThread extends TestThread {

        public PutLastThread(int id) {
            super("PutLastThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            for (int k = 0; k < produceCount; k++) {
                list.putLast(itemGenerator.incrementAndGet());

                if ((k % 100000) == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }
        }
    }
}
