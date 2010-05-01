package org.multiverse.integrationtests;

import org.junit.Before;
import org.junit.Test;

import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionRetriedOnFailureTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test
    public void test() {
    }
}
