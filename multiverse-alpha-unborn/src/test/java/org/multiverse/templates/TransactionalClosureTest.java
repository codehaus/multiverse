package org.multiverse.templates;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.transactional.primitives.TransactionalInteger;

import java.util.concurrent.Callable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

/**
 * @author Sai Venkat
 */

public class TransactionalClosureTest {
    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }
    @Test
    public void testSelfCreatedTransaction() {
        final TransactionalInteger value = new TransactionalInteger(0);

        long version = stm.getVersion();

        new TransactionalClosure().execute(
                new Callable() {
                    @Override
                    public Object call(){
                        value.inc();
                        return null;
                    }
                }
        );

        assertEquals(version + 1, stm.getVersion());
        assertNull(getThreadLocalTransaction());
        assertEquals(1, value.get());
    }
    
}
