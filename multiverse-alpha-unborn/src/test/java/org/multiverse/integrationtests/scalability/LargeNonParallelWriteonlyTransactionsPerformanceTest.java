package org.multiverse.integrationtests.scalability;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.templates.TransactionTemplate;
import org.multiverse.transactional.refs.IntRef;

import java.util.ArrayList;
import java.util.List;

import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class LargeNonParallelWriteonlyTransactionsPerformanceTest {
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
    public void test_1() {
        test(1);
    }

    @Test
    public void test_10() {
        test(10);
    }

    @Test
    public void test_100() {
        test(100);
    }

    @Test
    public void test_1000() {
        test(1000);
    }

    @Test
    public void test_10000() {
        test(10000);
    }

    @Test
    public void test_100000() {
        test(100000);
    }

    @Test
    public void test_1000000() {
        test(1000000);
    }

    //@Test
    public void test_10000000() {
        test(10000000);
    }

    public void test(final int x) {
        final List<IntRef> list = new ArrayList<IntRef>(x);

        TransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setReadonly(false)
                .setFamilyName(getClass().getName() + ".test()")
                .setSpeculativeConfigurationEnabled(true)
                .setReadTrackingEnabled(false).build();

        new TransactionTemplate(txFactory) {
            @Override
            public Object execute(Transaction tx) {
                for (int k = 0; k < x; k++) {
                    IntRef value = new IntRef(k);
                    list.add(value);
                }
                return null;
            }
        }.execute();
    }

}
