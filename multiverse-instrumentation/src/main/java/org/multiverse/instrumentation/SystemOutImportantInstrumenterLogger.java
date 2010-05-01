package org.multiverse.instrumentation;

import static org.multiverse.utils.SystemOut.println;

/**
 * @author Peter Veentjer
 */
public class SystemOutImportantInstrumenterLogger implements InstrumenterLogger {


    @Override
    public void important(String msg, Object... args) {
        println(msg, args);
    }


    @Override
    public void lessImportant(String msg, Object... args) {
    }
}
