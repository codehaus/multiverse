package org.multiverse.actors;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ImprovedBlockingQueueTest {

    @Test
    public void test() throws InterruptedException {
        ImprovedBlockingQueue<String> queue = new ImprovedBlockingQueue<String>(10);
        queue.put("1");
        queue.put("2");
        queue.put("3");

        assertEquals("1", queue.take());
        assertEquals("2", queue.take());
        assertEquals("3", queue.take());
    }
}
