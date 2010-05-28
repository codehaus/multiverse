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
        assertTrue(TraceLevel.fine.isLogableFrom(TraceLevel.fine));
        assertTrue(TraceLevel.fine.isLogableFrom(TraceLevel.course));
        assertFalse(TraceLevel.course.isLogableFrom(TraceLevel.fine));
        assertFalse(TraceLevel.none.isLogableFrom(TraceLevel.course));
    }
}
