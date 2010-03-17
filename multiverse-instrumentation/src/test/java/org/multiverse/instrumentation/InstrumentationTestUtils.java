package org.multiverse.instrumentation;

import org.multiverse.javaagent.JavaAgentProblemMonitor;

import java.lang.reflect.Field;

import static org.junit.Assert.assertFalse;

/**
 * @author Peter Veentjer
 */
public class InstrumentationTestUtils {

    public static void resetInstrumentationProblemMonitor() {
        try {
            Field field = JavaAgentProblemMonitor.class.getDeclaredField("problemFound");
            field.setAccessible(true);
            field.set(JavaAgentProblemMonitor.INSTANCE, false);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void assertNoInstrumentationProblems() {
        assertFalse(JavaAgentProblemMonitor.INSTANCE.isProblemFound());
    }
}
