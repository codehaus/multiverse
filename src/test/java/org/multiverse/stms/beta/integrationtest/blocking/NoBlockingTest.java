package org.multiverse.stms.beta.integrationtest.blocking;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.exceptions.NoRetryPossibleException;
import org.multiverse.api.references.LongRef;

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
    public void whenNothingRead_thenNoRetryPossibleException(){
        try {
            execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    retry();
                }
            });
            fail();
        } catch (NoRetryPossibleException expected){
        }
    }

    @Test
    public void whenBlockingNotAllowed_thenNoRetryPossibleException() {
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
        } catch (NoRetryPossibleException expected){
        }

        assertEquals(0, ref.atomicGet());
    }
}
