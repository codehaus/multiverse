package org.multiverse.integrationtests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.transactional.primitives.TransactionalLong;

import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class ContendingStressTest {
    private Stm stm;

    private int refCount = 100;
    private TransactionalLong[] refs;
    private int threadCount;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();

        for (int k = 0; k < refCount; k++) {
            //    refs[k] = new TransactionalLong();
        }
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void test() {

    }
}
