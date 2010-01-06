package org.multiverse.datastructures.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import org.multiverse.api.Stm;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

import java.util.LinkedList;

/**
 * @author Peter Veentjer
 */
public class TransactionalLinkedList_addAllWithIndex_Test {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void callFailsWithUnsupportedOperationException() {
        TransactionalLinkedList<String> l = new TransactionalLinkedList<String>();

        long version = stm.getTime();
        try {
            l.addAll(1, new LinkedList<String>());
            fail();
        } catch (UnsupportedOperationException expected) {
        }

        assertEquals(version, stm.getTime());
        assertEquals("[]", l.toString());
    }

}
