package org.multiverse.stms.alpha.manualinstrumentation;

import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;

import static org.junit.Assert.assertEquals;

public class SizeTest {

    @Test
    public void testConstruction() {
        Size size = new Size();
        assertEquals(0, size.get());
    }

    @Test
    public void testSetTest() {
        Size size = new Size();
        size.set(10);
        assertEquals(10, size.get());
    }

    @Test
    public void testIncAfterSet() {
        Size size = new Size();
        doit(size);
        assertEquals(11, size.get());
    }

    @TransactionalMethod
    private void doit(Size size) {
        size.set(10);
        size.inc();
    }
}
