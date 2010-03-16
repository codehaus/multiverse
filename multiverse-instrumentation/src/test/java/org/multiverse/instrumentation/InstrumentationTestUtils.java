package org.multiverse.instrumentation;

import java.lang.reflect.Field;

import static org.junit.Assert.assertFalse;

/**
 * @author Peter Veentjer
 */
public class InstrumentationTestUtils {

    public static void resetInstrumentationProblemMonitor() {
        try {
            Field field = InstrumentationProblemMonitor.class.getDeclaredField("problemFound");
            field.setAccessible(true);
            field.set(InstrumentationProblemMonitor.INSTANCE, false);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void assertNoInstrumentationProblems() {
        assertFalse(InstrumentationProblemMonitor.INSTANCE.isProblemFound());
    }
}
