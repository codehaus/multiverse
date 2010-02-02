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
 * todo: once the time consuming structure checks in the transactionallinkedlist have been removed, the produceCount
 * needs to be increased
 *
 * @author Peter Veentjer
 */
public class TransactionalLinkedList_TakeLastPutFirstStressTest {

    private int takeFirstThreadCount = 2;
    private int putLastThreadCount = 2;
    private int dequeCapacity;
    private int produceCount = 2000 * 1000;

    private AtomicLong itemGenerator;
    private AtomicLong takeCounter;
    private TransactionalLinkedList<Long> list;
    private PutFirstThread[] putLastThreads;
    private TakeLastThread[] takeFirstThreads;

    @Before
    public void setUp() {
        itemGenerator = new AtomicLong();
        takeCounter = new AtomicLong(putLastThreadCount * produceCount);

        list = new TransactionalLinkedList<Long>(dequeCapacity);

        putLastThreads = new PutFirstThread[putLastThreadCount];
        for (int k = 0; k < putLastThreadCount; k++) {
            putLastThreads[k] = new PutFirstThread(k);
        }

        takeFirstThreads = new TakeLastThread[takeFirstThreadCount];
        for (int k = 0; k < takeFirstThreadCount; k++) {
            takeFirstThreads[k] = new TakeLastThread(k);
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

    public class TakeLastThread extends TestThread {

        public TakeLastThread(int id) {
            super("TakeLastThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            int k = 0;
            while (takeCounter.decrementAndGet() > 0) {
                Long l = list.takeLast();
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

    public class PutFirstThread extends TestThread {

        public PutFirstThread(int id) {
            super("PutFirstThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            for (int k = 0; k < produceCount; k++) {
                list.putFirst(itemGenerator.incrementAndGet());

                if ((k % 100000) == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }
        }
    }
}
