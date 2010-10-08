package org.multiverse.actors;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class ImprovedBlockingQueueTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test
    public void test() throws InterruptedException {
        ImprovedBlockingQueue<String> queue = new ImprovedBlockingQueue<String>(10);
        queue.put("1");
        queue.put("2");
        queue.put("3");

        System.out.println(getThreadLocalTransaction());

        System.out.println("-----------------");
        assertEquals("1", queue.take());
        System.out.println("-----------------");
        assertEquals("2", queue.take());
        assertEquals("3", queue.take());
    }
}
