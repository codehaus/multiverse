package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeSet_hashCodeTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test
    @Ignore
    public void whenEmpty() {

    }

    @Test
    @Ignore
    public void whenNonEmpty() {
    }
}
