package org.multiverse.stms.beta.integrationtest.failureatomicity;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.exceptions.RetryTimeoutException;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.refs.IntRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.createIntRef;
import static org.multiverse.stms.beta.ThreadLocalBetaObjectPool.getThreadLocalBetaObjectPool;

public class TimeoutRollbackTest {
    private IntRef modifyRef;
    private IntRef awaitRef;
    private BetaStm stm;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = new BetaStm();
        modifyRef = createIntRef(stm);
        awaitRef = createIntRef(stm);
    }

    @Test
    public void test() {
        try {
            setAndTimeout();
            fail();
        } catch (RetryTimeoutException expected) {
        }

        assertEquals(0, modifyRef.unsafeLoad().value);
    }

    public void setAndTimeout() {
        AtomicBlock block = stm.getTransactionFactoryBuilder()
                .setTimeoutNs(TimeUnit.SECONDS.toNanos(1))
                .buildAtomicBlock();

        block.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                BetaTransaction btx = (BetaTransaction) tx;
                BetaObjectPool pool = getThreadLocalBetaObjectPool();
                modifyRef.set(btx, pool, 1);

                if (awaitRef.get(btx, pool) != 1000) {
                    retry();
                }
            }
        });
    }
}
