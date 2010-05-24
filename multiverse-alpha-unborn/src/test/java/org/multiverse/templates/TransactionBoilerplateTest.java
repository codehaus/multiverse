package org.multiverse.templates;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.transactional.refs.IntRef;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Sai Venkat
 */
public class TransactionBoilerplateTest {
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
        final IntRef value = new IntRef(0);

        long version = stm.getVersion();

        new TransactionBoilerplate().execute(
                new TransactionalCallable() {
                    @Override
                    public Object call(Transaction tx) {
                        value.inc();
                        return null;
                    }
                }
        );

        assertEquals(version + 1, stm.getVersion());
        assertEquals(1, value.get());
    }
}
