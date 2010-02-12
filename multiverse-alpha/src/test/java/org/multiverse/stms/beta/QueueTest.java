package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class QueueTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void constructor() {
        Queue queue = new Queue(stm);
        int size = queue.size(stm);
        assertEquals(0, size);
    }

    @Test
    public void push_whenQueueEmpty_itemAdded() {
        Queue queue = new Queue(stm);
        String item = "foo";
        queue.push(item, stm);
        int size = queue.size(stm);
        assertEquals(1, size);
    }

    @Test(expected = IllegalStateException.class)
    public void pop_whenQueueEmpty() {
        Queue queue = new Queue(stm);
        queue.pop(stm);
    }

    @Test
    public void pop_whenQueueNotEmpty() {
        Queue queue = new Queue(stm);
        String item = "foo";
        queue.push(item, stm);

        String found = queue.pop(stm);
        assertSame(item, found);
        assertEquals(0, queue.size(stm));
    }

    @Test
    public void complex() {
        Queue queue = new Queue(stm);
        String item1 = "1";
        String item2 = "1";
        String item3 = "1";

        queue.push(item1, stm);
        queue.push(item2, stm);
        queue.push(item3, stm);

        assertSame(item1, queue.pop(stm));
        assertEquals(2, queue.size(stm));

        assertSame(item2, queue.pop(stm));
        assertEquals(1, queue.size(stm));

        assertSame(item3, queue.pop(stm));
        assertEquals(0, queue.size(stm));
    }
}
