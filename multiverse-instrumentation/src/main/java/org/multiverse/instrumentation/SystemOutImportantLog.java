package org.multiverse.instrumentation;

/**
 * @author Peter Veentjer
 */
public class SystemOutImportantLog implements Log {


    @Override
    public void important(String msg, Object... args) {
        System.out.printf(msg + "\n", args);
    }

    @Override
    public void lessImportant(String msg, Object... args) {
        //    System.out.printf(msg + "\n", args);
    }
}
