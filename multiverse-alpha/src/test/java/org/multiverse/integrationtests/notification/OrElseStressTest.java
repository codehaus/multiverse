package org.multiverse.integrationtests.notification;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.TestUtils;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.Transaction;
import org.multiverse.templates.OrElseTemplate;
import org.multiverse.transactional.primitives.TransactionalInteger;

import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.api.StmUtils.retry;

public class OrElseStressTest {

    private int waitCountPerWaiter = 10000;
    private int waitingThreadCount = 20;
    private int refCount = 100;

    private TransactionalInteger[] refs;
    private WaitingThread[] waitingThreads;
    private NotifyThread notifyThread;

    @Before
    public void setUp() {
        refs = new TransactionalInteger[refCount];
        for (int k = 0; k < refCount; k++) {
            refs[k] = new TransactionalInteger();
        }

        waitingThreads = new WaitingThread[waitingThreadCount];
        for (int k = 0; k < waitingThreads.length; k++) {
            waitingThreads[k] = new WaitingThread(k);
        }

        notifyThread = new NotifyThread();
    }

    @Test
    public void test() {
        startAll(notifyThread);
        startAll(waitingThreads);
        joinAll(waitingThreads);
        joinAll(notifyThread);
    }

    class NotifyThread extends TestThread {

        public NotifyThread() {
            super("NotifyThread");
        }

        @Override
        public void doRun() throws Exception {
            for (int k = 0; k < waitCountPerWaiter * waitingThreadCount; k++) {
                if (k % 1000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }

                //System.out.println("start await");
                awaitAllZero();
                //System.out.println("finished await");


                int randomIndex = TestUtils.randomInt(refs.length - 1);
                refs[randomIndex].inc();
            }
        }

        @TransactionalMethod(readonly = false, automaticReadTracking = true)
        public void awaitAllZero() {
            for (int k = 0; k < refs.length; k++) {
                if (refs[k].get() == 1) {
                    retry();
                }
            }
        }
    }

    class WaitingThread extends TestThread {

        public WaitingThread(int id) {
            super("WaitingThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            for (int k = 0; k < waitCountPerWaiter; k++) {
                if (k % 1000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
                decreaseRefContainingOne(0);
            }
        }

        @TransactionalMethod
        public void decreaseRefContainingOne(final int refIndex) {
            new OrElseTemplate() {
                @Override
                public Object run(Transaction tx) {
                    //System.out.println("++++++++++++++++++++++++++");
                    //System.out.println(tx.getClass()+" "+System.identityHashCode(tx));
                    refs[refIndex].await(1);
                    //System.out.println("--------------------------");
                    refs[refIndex].dec();
                    return null;
                }

                @Override
                public Object orelserun(Transaction t) {
                    if (refIndex == refs.length - 1) {
                        retry();
                    } else {
                        decreaseRefContainingOne(refIndex + 1);
                    }
                    return null;
                }
            }.execute();
        }
    }
}


