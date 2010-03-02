package org.multiverse.transactional.collections;

import org.junit.Test;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.multiverse.TestUtils.hasField;
import static org.multiverse.TestUtils.hasMethod;

/**
 * @author Peter Veentjer
 */
public class TransactionalLinkedList_ClassStructureTest {

    @Test
    public void test() {
        Class clazz = TransactionalLinkedList.class;

        assertTrue(hasField(clazz, "maxCapacity"));
        assertFalse(hasField(clazz, "size"));
        assertFalse(hasField(clazz, "head"));
        assertFalse(hasField(clazz, "tail"));

        assertTrue(hasMethod(clazz, "remainingCapacity"));
        assertTrue(hasMethod(clazz, "remainingCapacity", AlphaTransaction.class));
    }

}
