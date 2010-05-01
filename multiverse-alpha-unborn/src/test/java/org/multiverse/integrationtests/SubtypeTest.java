package org.multiverse.integrationtests;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.transactional.collections.TransactionalLinkedList;
import org.multiverse.transactional.collections.TransactionalList;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class SubtypeTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test
    public void whenNonTransactionalInterfaceInvoked() {
        List list = new TransactionalLinkedList();
        list.add("foo");
        assertEquals("[foo]", list.toString());
    }

    @Test
    public void whenNonInterfaceInvokedInsideTransactionalMethod() {
        List list = new TransactionalLinkedList();

        addFooToNormalList(list);

        assertEquals("[foo]", list.toString());
    }


    @TransactionalMethod
    public void addFooToNormalList(List list) {
        list.add("foo");
    }

    @Test
    public void whenTransactionalInterfaceInvoked() {
        TransactionalList list = new TransactionalLinkedList();
        list.add("foo");

        assertEquals("[foo]", list.toString());
    }

    @Test
    public void whenTransactionalInterfaceInvokedInsideTransactionalMethod() {
        TransactionalLinkedList list = new TransactionalLinkedList();

        addFooToTransactionalList(list);

        assertEquals("[foo]", list.toString());
    }

    @TransactionalMethod
    public void addFooToTransactionalList(TransactionalList list) {
        list.add("foo");
    }

}
