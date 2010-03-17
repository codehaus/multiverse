package org.multiverse.instrumentation.compiler;

/**
 * @author Peter Veentjer
 */
public class SystemOutLog implements Log {

    @Override
    public void important(String msg, Object... args) {
        System.out.printf(msg + "\n", args);
    }

    @Override
    public void lessImportant(String msg, Object... args) {
        System.out.printf(msg + "\n", args);
    }
}
