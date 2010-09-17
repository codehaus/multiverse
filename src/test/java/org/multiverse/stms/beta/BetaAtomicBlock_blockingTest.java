package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;

public class BetaAtomicBlock_blockingTest {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void test() {
        final BetaLongRef ref = newLongRef(stm);

        WaitThread t = new WaitThread(ref);
        t.start();

        sleepMs(1000);
        assertAlive(t);

        stm.getDefaultAtomicBlock().execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                BetaTransaction btx = (BetaTransaction) tx;
                LongRefTranlocal write = btx.openForWrite(ref, false);
                write.value = 1;
            }
        });

        joinAll(t);
        assertEquals(2, ref.___unsafeLoad().value);
    }

    class WaitThread extends TestThread {
        final BetaLongRef ref;

        public WaitThread(BetaLongRef ref) {
            this.ref = ref;
        }

        @Override
        public void doRun() throws Exception {
            stm.getDefaultAtomicBlock().execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    LongRefTranlocal write = btx.openForWrite(ref, false);
                    if (write.value == 0) {
                        retry();
                    }

                    write.value++;
                }
            });
        }
    }
}
