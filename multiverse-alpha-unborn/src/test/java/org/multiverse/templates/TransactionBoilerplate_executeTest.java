package org.multiverse.templates;

import org.junit.Before;
import org.junit.Test;

import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionBoilerplate_executeTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test(expected = NullPointerException.class)
    public void whenNullArgument_thenNullPointerException() {
        TransactionBoilerplate boilerplate = new TransactionBoilerplate();
        boilerplate.execute(null);
    }
}
