package org.multiverse.commitbarriers;

import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.Transaction;

import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class JoinCommitThread extends TestThread {
    private final CountDownCommitBarrier barrier;

    JoinCommitThread(CountDownCommitBarrier barrier) {
        this.barrier = barrier;
    }

    @Override
    @TransactionalMethod
    public void doRun() throws Exception {
        Transaction tx = getThreadLocalTransaction();
        barrier.joinCommit(tx);
    }
}
