package org.multiverse.stms.gamma.integration.blocking;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.exceptions.RetryNotAllowedException;
import org.multiverse.api.exceptions.RetryNotPossibleException;
import org.multiverse.api.functions.Functions;
import org.multiverse.api.references.LongRef;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class NoBlockingTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test
    public void whenNothingRead_thenNoRetryPossibleException() {
        try {
            execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    retry();
                }
            });
            fail();
        } catch (RetryNotPossibleException expected) {
        }
    }

    @Test
    public void whenContainsCommute_thenNoRetryPossibleException() {
        final LongRef ref = newLongRef();

        try {
            execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    ref.commute(Functions.newIncLongFunction());
                    retry();
                }
            });
            fail();
        } catch (RetryNotPossibleException expected) {
        }
    }

    @Test
    public void whenContainsConstructing_thenNoRetryPossibleException() {
        try {
            execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    BetaLongRef ref = new BetaLongRef(btx);
                    btx.openForConstruction(ref);
                    retry();
                }
            });
            fail();
        } catch (RetryNotPossibleException expected) {
        }
    }

    @Test
    public void whenBlockingNotAllowed_thenNoBlockingRetryAllowedException() {
        final LongRef ref = newLongRef();

        AtomicBlock block = getGlobalStmInstance()
                .createTransactionFactoryBuilder()
                .setBlockingAllowed(false)
                .buildAtomicBlock();

        try {
            block.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    ref.set(1);
                    retry();
                }
            });
            fail();
        } catch (RetryNotAllowedException expected) {
        }

        assertEquals(0, ref.atomicGet());
    }
}
