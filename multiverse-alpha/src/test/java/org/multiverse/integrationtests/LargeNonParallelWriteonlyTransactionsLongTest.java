package org.multiverse.integrationtests;

import static junit.framework.Assert.assertEquals;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import org.multiverse.api.Transaction;
import org.multiverse.datastructures.refs.IntRef;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.templates.AtomicTemplate;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

import java.util.ArrayList;
import java.util.List;

public class LargeNonParallelWriteonlyTransactionsLongTest {
    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = AlphaStm.createDebug();
        setGlobalStmInstance(stm);
        setThreadLocalTransaction(null);
    }

    @After
    public void tearDown() {
        //  stm.getProfiler().print();
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

        new AtomicTemplate() {
            @Override
            public Object execute(Transaction t) {
                for (int k = 0; k < x; k++) {
                    IntRef value = new IntRef(k);
                    list.add(value);
                }
                return null;
            }
        }.execute();

        assertEquals(0, stm.getProfiler().sumKey1("updatetransaction.failedtoacquirelocks.count"));
        assertEquals(0, stm.getProfiler().sumKey1("updatetransaction.writeconflict.count"));
        assertEquals(1, stm.getProfiler().sumKey1("updatetransaction.committed.count"));
        assertEquals(0, stm.getProfiler().sumKey1("updatetransaction.retried.count"));
    }

}
