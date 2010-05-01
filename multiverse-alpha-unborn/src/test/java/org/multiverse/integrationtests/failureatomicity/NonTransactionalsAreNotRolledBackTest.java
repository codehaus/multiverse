package org.multiverse.integrationtests.failureatomicity;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class NonTransactionalsAreNotRolledBackTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test
    @Ignore
    public void test() {
    }
}
