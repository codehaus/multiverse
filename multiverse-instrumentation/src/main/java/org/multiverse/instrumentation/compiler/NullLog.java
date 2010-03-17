package org.multiverse.instrumentation.compiler;

/**
 * The NullLog is a Log that doesn't do anything. It is useful if you don't want to have logging.
 *
 * @author Peter Veentjer
 */
public class NullLog implements Log {

    @Override
    public void important(String msg, Object... args) {
    }

    @Override
    public void lessImportant(String msg, Object... args) {
    }
}
