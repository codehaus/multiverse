package org.multiverse.templates;

import org.junit.Before;
import org.junit.Test;

import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionBoilerplate_executeCheckedTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test(expected = NullPointerException.class)
    public void whenNullArgument_thenNullPointerException() throws Exception {
        TransactionBoilerplate boilerplate = new TransactionBoilerplate();
        boilerplate.executeChecked(null);
    }
}
