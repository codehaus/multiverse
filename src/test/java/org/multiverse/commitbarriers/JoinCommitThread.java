package org.multiverse.commitbarriers;

import org.multiverse.TestThread;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.stms.beta.BetaStm;

import static org.junit.Assert.assertNotNull;

/**
 * @author Peter Veentjer
 */
public class JoinCommitThread extends TestThread {
    private final CountDownCommitBarrier barrier;
    private final BetaStm stm;

    public JoinCommitThread(BetaStm stm, CountDownCommitBarrier barrier) {
        this.barrier = barrier;
        this.stm = stm;
    }

    @Override
    public void doRun() throws Exception {
        stm.getDefaultAtomicBlock().execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                assertNotNull(tx);
                barrier.joinCommit(tx);
            }
        });
    }
}
