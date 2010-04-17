package org.multiverse.instrumentation;

/**
 * The NullInstrumenterLogger is a InstrumenterLogger that doesn't do anything. It is useful if you don't want to have logging.
 *
 * @author Peter Veentjer
 */
public class NullInstrumenterLogger implements InstrumenterLogger {

    @Override
    public void important(String msg, Object... args) {
    }

    @Override
    public void lessImportant(String msg, Object... args) {
    }
}
