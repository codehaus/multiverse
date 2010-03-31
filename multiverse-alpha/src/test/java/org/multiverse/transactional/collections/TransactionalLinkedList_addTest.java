package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class TransactionalLinkedList_addTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void addWithNullFails() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();

        long version = stm.getVersion();
        try {
            list.add(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertEquals(0, list.size());
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void addOnEmptyDeque() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();

        long version = stm.getVersion();
        System.out.println("------------------------------------------");
        boolean result = list.add("1");
        System.out.println("------------------------------------------");
        assertTrue(result);
        assertEquals(version + 1, stm.getVersion());
        assertEquals("[1]", list.toString());
    }

    @Test
    public void addOnNonEmptyDeque() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");

        long version = stm.getVersion();
        boolean result = list.add("2");
        assertTrue(result);
        assertEquals(version + 1, stm.getVersion());
        assertEquals("[1, 2]", list.toString());
    }

    @Test
    public void addOnFullDeque() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>(1);
        list.add("1");

        long version = stm.getVersion();

        try {
            list.add("2");
            fail();
        } catch (IllegalStateException ex) {
        }

        assertEquals(version, stm.getVersion());
        assertEquals("[1]", list.toString());
        assertEquals(1, list.size());
    }

    @Test
    public void addVeryMany(){
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        int count = 10000;

        for(int k=0;k< count;k++){
            list.add(""+k);
        }

        assertEquals(list.size(), count);
        for(int k=0;k<count;k++){
            assertEquals(""+k, list.get(k));            
        }
    }
}
