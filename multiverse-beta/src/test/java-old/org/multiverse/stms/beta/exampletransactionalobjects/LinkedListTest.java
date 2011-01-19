package org.multiverse.stms.beta.exampletransactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertNull;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

@Ignore
public class LinkedListTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void testConstruction() {
        BetaTransaction tx = stm.startDefaultTransaction();
        LinkedList list = new LinkedList(tx);
        tx.openForConstruction(list);
        tx.commit();
    }

    @Test
    public void addAndRemoveInBack() {
        LinkedList<String> list = createLinkedList();

        BetaTransaction tx = stm.startDefaultTransaction();
        list.addInBack(tx, "1");
        tx.commit();

        tx = stm.startDefaultTransaction();
        list.addInBack(tx, "2");
        tx.commit();

        tx = stm.startDefaultTransaction();
        assertEquals("2", list.removeFromBack(tx));
        tx.commit();

        tx = stm.startDefaultTransaction();
        assertEquals("1", list.removeFromBack(tx));
        tx.commit();

        tx = stm.startDefaultTransaction();
        assertNull(list.removeFromBack(tx));
        tx.commit();
    }

    @Test
    public void addAndRemoveInFront() {
        LinkedList<String> list = createLinkedList();

        BetaTransaction tx = stm.startDefaultTransaction();
        list.addInFront(tx, "1");
        tx.commit();

        tx = stm.startDefaultTransaction();
        list.addInFront(tx, "2");
        tx.commit();

        tx = stm.startDefaultTransaction();
        assertEquals("2", list.removeFromFront(tx));
        tx.commit();

        tx = stm.startDefaultTransaction();
        assertEquals("1", list.removeFromFront(tx));
        tx.commit();

        tx = stm.startDefaultTransaction();
        assertNull(list.removeFromFront(tx));
        tx.commit();
    }

    private <E> LinkedList<E> createLinkedList() {
        BetaTransaction tx = stm.startDefaultTransaction();
        LinkedList<E> list = new LinkedList<E>(tx);
        tx.openForConstruction(list);
        tx.commit();
        return list;
    }
}
