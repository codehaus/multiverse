package org.multiverse.templates;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.transactional.refs.IntRef;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * Test to make sure that the TransactionTemplate is able to deal with growing transactions. Growing
 * transaction could lead to TransactionTooSmallExceptions from time to time.
 *
 * @author Peter Veentjer.
 */
public class TransactionTemplate_growingTransactionsTest {
    private Stm stm;
    private int refCount = 100 * 1000;
    private IntRef[] refs;
    private TransactionFactory txFactory;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();

        txFactory = stm.getTransactionFactoryBuilder()
                .setReadonly(false)
                .setFamilyName(getClass().getName())
                .setSpeculativeConfigurationEnabled(true)
                .build();

        refs = new IntRef[refCount];

        for (int k = 0; k < refs.length; k++) {
            refs[k] = new IntRef();
        }
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void test() {
        new TransactionTemplate(txFactory) {
            @Override
            public Object execute(Transaction t) throws Exception {
                for (int k = 0; k < refs.length; k++) {
                    refs[k].inc();
                }
                return null;
            }
        }.execute();

        //make sure that every reference is set to 1.
        for (int k = 0; k < refs.length; k++) {
            assertEquals(1, refs[k].get());
        }
    }
}
