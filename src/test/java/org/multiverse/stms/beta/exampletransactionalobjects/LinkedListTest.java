package org.multiverse.stms.beta.exampletransactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class LinkedListTest {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
        clearThreadLocalTransaction();
    }

    @Test
    public void testConstruction() {
        BetaTransaction tx = stm.startDefaultTransaction();
        LinkedList list = new LinkedList(tx);
        tx.openForConstruction(list, pool);
        tx.commit();
    }

        @Test
    public void addAndRemoveInBack() {
        LinkedList<String> list = createLinkedList();

        BetaTransaction tx = stm.startDefaultTransaction();
        list.addInBack(tx, pool, "1");
        tx.commit();

        tx = stm.startDefaultTransaction();
        list.addInBack(tx, pool, "2");
        tx.commit();

        tx = stm.startDefaultTransaction();
        assertEquals("2", list.removeFromBack(tx, pool));
        tx.commit();

        tx = stm.startDefaultTransaction();
        assertEquals("1", list.removeFromBack(tx, pool));
        tx.commit();

        tx = stm.startDefaultTransaction();
        assertNull(list.removeFromBack(tx, pool));
        tx.commit();
    }


    @Test
    public void addAndRemoveInFront() {
        LinkedList<String> list = createLinkedList();

        BetaTransaction tx = stm.startDefaultTransaction();
        list.addInFront(tx, pool, "1");
        tx.commit();

        tx = stm.startDefaultTransaction();
        list.addInFront(tx, pool, "2");
        tx.commit();

        tx = stm.startDefaultTransaction();
        assertEquals("2", list.removeFromFront(tx, pool));
        tx.commit();

        tx = stm.startDefaultTransaction();
        assertEquals("1", list.removeFromFront(tx, pool));
        tx.commit();

        tx = stm.startDefaultTransaction();
        assertNull(list.removeFromFront(tx, pool));
        tx.commit();
    }

    private <E> LinkedList<E> createLinkedList() {
        BetaTransaction tx = stm.startDefaultTransaction();
        LinkedList<E> list = new LinkedList<E>(tx);
        tx.openForConstruction(list, pool);
        tx.commit();
        return list;
    }
}
