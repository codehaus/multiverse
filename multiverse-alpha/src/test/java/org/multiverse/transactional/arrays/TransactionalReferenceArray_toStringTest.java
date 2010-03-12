package org.multiverse.transactional.arrays;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Veentjer
 */
public class TransactionalReferenceArray_toStringTest {

    @Test
    public void whenNoElements() {
        TransactionalReferenceArray array = new TransactionalReferenceArray(0);
        String result = array.toString();
        assertEquals("[]", result);
    }

    @Test
    public void whenSomeElements() {
        TransactionalReferenceArray array = new TransactionalReferenceArray(3);
        array.set(0, "zero");
        array.set(1, "one");
        array.set(2, "two");

        String result = array.toString();
        assertEquals("[zero, one, two]", result);
    }

    @Test
    public void whenNullElement() {
        TransactionalReferenceArray array = new TransactionalReferenceArray(1);

        String result = array.toString();
        assertEquals("[null]", result);
    }
}
