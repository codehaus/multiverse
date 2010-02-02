package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import java.util.LinkedList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

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
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();

        long version = stm.getVersion();
        try {
            list.addAll(1, new LinkedList<String>());
            fail();
        } catch (UnsupportedOperationException expected) {
        }

        assertEquals(version, stm.getVersion());
        assertEquals("[]", list.toString());
    }

}
