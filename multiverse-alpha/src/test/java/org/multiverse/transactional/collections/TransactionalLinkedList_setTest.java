package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class TransactionalLinkedList_setTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void setFailsIfIndexOutOfBounds() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");

        setFailsIfIndexOutOfBounds(list, -1);
        setFailsIfIndexOutOfBounds(list, 1);
        setFailsIfIndexOutOfBounds(list, 2);
    }

    private void setFailsIfIndexOutOfBounds(TransactionalLinkedList<String> list, int index) {
        String original = list.toString();
        long version = stm.getVersion();
        try {
            list.get(index);
            fail();
        } catch (IndexOutOfBoundsException ignore) {
        }
        assertEquals(version, stm.getVersion());
        assertEquals(original, list.toString());
    }

    @Test
    public void set() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");
        list.add("2");
        list.add("3");

        String result = list.set(0, "a");
        assertEquals(result, "1");
        assertEquals("[a, 2, 3]", list.toString());

        result = list.set(1, "b");
        assertEquals(result, "2");
        assertEquals("[a, b, 3]", list.toString());
    }
}
