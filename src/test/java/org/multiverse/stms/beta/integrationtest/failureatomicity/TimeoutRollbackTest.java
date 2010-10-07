package org.multiverse.stms.beta.integrationtest.failureatomicity;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.exceptions.RetryTimeoutException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmUtils;
import org.multiverse.stms.beta.transactionalobjects.BetaIntRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmTestUtils.newIntRef;

public class TimeoutRollbackTest {
    private BetaIntRef modifyRef;
    private BetaIntRef awaitRef;
    private BetaStm stm;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = new BetaStm();
        modifyRef = newIntRef(stm);
        awaitRef = newIntRef(stm);
    }

    @Test
    public void test() {
        try {
            setAndTimeout();
            fail();
        } catch (RetryTimeoutException expected) {
        }

        assertEquals(0, modifyRef.atomicGet());
    }

    public void setAndTimeout() {
        AtomicBlock block = stm.createTransactionFactoryBuilder()
                .setTimeoutNs(TimeUnit.SECONDS.toNanos(1))
                .buildAtomicBlock();

        block.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                BetaTransaction btx = (BetaTransaction) tx;
                modifyRef.getAndSet(btx, 1);

                if (awaitRef.get(btx) != 1000) {
                    retry();
                }
            }
        });
    }
}
