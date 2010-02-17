package org.multiverse.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class StandardThreadFactory_createTest {

    @Test
    public void testArguments() {
        StandardThreadFactory factory = new StandardThreadFactory();

        try {
            factory.newThread(null);
            fail("NullPointerException expected");
        } catch (NullPointerException ex) {
            assertTrue(true);
        }
    }

    @Test
    public void testPriorityIsHigherThanThreadgroupAllows() {
        ThreadGroup threadGroup = new ThreadGroup("somename");
        threadGroup.setMaxPriority(Thread.MAX_PRIORITY);
        StandardThreadFactory factory = new StandardThreadFactory(Thread.MAX_PRIORITY, threadGroup);
        threadGroup.setMaxPriority(Thread.MAX_PRIORITY - 1);

        Runnable task = new Runnable() {
            public void run() {
            }
        };

        //todo: extra checks needed
        factory.newThread(task);
    }

    @Test
    public void testSuccess() {
        ThreadGroup group = new ThreadGroup("somename");
        int priority = Thread.MAX_PRIORITY;
        StandardThreadFactory factory = new StandardThreadFactory(priority, group);
        Runnable task = new Runnable() {
            public void run() {
            }
        };

        Thread t = factory.newThread(task);
        assertNotNull(t);
        assertEquals(priority, t.getPriority());
        assertSame(group, t.getThreadGroup());
        assertEquals(Thread.State.NEW, t.getState());
        assertFalse(t.isDaemon());
    }
}
