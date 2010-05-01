package org.multiverse.integrationtests.failureatomicity;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.transactional.collections.TransactionalLinkedList;
import org.multiverse.transactional.collections.TransactionalList;

import java.util.LinkedList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class NonTransactionalsAreNotRolledBackTest {
    private LinkedList nonTransactional;
    private TransactionalList transactional;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        nonTransactional = new LinkedList();
        transactional = new TransactionalLinkedList();
    }

    @Test
    public void test() {
        try {
            failingAdd();
            fail();
        } catch (UncheckedException expected) {
        }

        assertEquals(1, nonTransactional.size());
        assertEquals(0, transactional.size());
    }

    @TransactionalMethod(readonly = false)
    public void failingAdd() {
        transactional.add("1");
        nonTransactional.add("1");
        throw new UncheckedException();
    }

    class UncheckedException extends RuntimeException {
    }
}
