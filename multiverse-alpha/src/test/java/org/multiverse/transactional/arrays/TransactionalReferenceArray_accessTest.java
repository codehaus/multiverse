package org.multiverse.transactional.arrays;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TransactionalReferenceArray_accessTest {

    @Test(expected = IndexOutOfBoundsException.class)
    public void set_whenIndexTooSmall_thenIndexOutOfBoundsException() {
        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(5);
        array.set(-1, "foo");
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void set_whenIndexTooBig_thenIndexOutOfBoundsException() {
        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(5);
        array.set(6, "foo");
    }

    @Test
    public void set_whenEmptyCellWritten() {
        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(5);
        String result = array.set(4, "foo");

        assertNull(result);
        assertEquals("foo", array.get(4));
    }

    @Test
    public void set_whenOverwrite() {
        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(5);
        array.set(4, "foo");

        String result = array.set(4, "bar");
        assertEquals("foo", result);
        assertEquals("bar", array.get(4));
    }

    @Test
    public void get_readingEmptyCell() {
        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(5);

        String result = array.get(3);
        assertNull(result);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void get_whenIndexTooSmall_thenIndexOutOfBoundsException() {
        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(5);
        array.get(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void get_whenIndexTooBig_thenIndexOutOfBoundsException() {
        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(5);
        array.get(6);
    }
}
