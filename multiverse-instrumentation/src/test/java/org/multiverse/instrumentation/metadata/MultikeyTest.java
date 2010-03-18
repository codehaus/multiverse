package org.multiverse.instrumentation.metadata;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Veentjer
 */
public class MultikeyTest {

    @Test
    public void test() {
        Multikey key = new Multikey();
        assertFalse(key.equals(null));
        assertTrue(key.equals(key));
    }

    @Test
    public void whenNoFields() {
        Multikey key1 = new Multikey();
        Multikey key2 = new Multikey();

        assertTrue(key1.equals(key2));
        assertEquals(key1.hashCode(), key2.hashCode());
    }

    @Test
    public void whenFieldsMatch() {
        Multikey key1 = new Multikey("a");
        Multikey key2 = new Multikey("a");

        assertTrue(key1.equals(key2));
        assertEquals(key1.hashCode(), key2.hashCode());
    }

    @Test
    public void whenFieldsNotMatch() {
        Multikey key1 = new Multikey("a");
        Multikey key2 = new Multikey("b");

        assertFalse(key1.equals(key2));
        assertTrue(!((Integer) key1.hashCode()).equals(key2.hashCode()));
    }

    @Test
    public void whenDifferenceInFieldNumber() {
        Multikey key1 = new Multikey("a");
        Multikey key2 = new Multikey("a", "a");

        assertFalse(key1.equals(key2));
        assertTrue(!((Integer) key1.hashCode()).equals(key2.hashCode()));
    }

    @Test
    public void testToString() {
        Multikey key = new Multikey("a", "b", "c");

        assertEquals("Multikey(a, b, c)", key.toString());
    }
}
