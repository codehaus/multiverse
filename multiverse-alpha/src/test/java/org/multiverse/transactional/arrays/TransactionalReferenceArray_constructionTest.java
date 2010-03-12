package org.multiverse.transactional.arrays;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TransactionalReferenceArray_constructionTest {

    @Test(expected = IllegalArgumentException.class)
    public void whenSizeNegative_thenNegativeArraySizeException() {
        new TransactionalReferenceArray<String>(-1);
    }

    @Test
    public void whenConstructed() {
        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(10);
        assertEquals(10, array.length());
    }
}
