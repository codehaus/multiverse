package org.multiverse.api;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Veentjer
 */
public class LogLevelTest {

    @Test
    public void test() {
        assertTrue(LogLevel.fine.isLogableFrom(LogLevel.fine));
        assertTrue(LogLevel.fine.isLogableFrom(LogLevel.course));
        assertFalse(LogLevel.course.isLogableFrom(LogLevel.fine));
        assertFalse(LogLevel.none.isLogableFrom(LogLevel.course));
    }
}
