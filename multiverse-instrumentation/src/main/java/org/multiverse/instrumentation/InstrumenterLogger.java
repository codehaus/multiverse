package org.multiverse.instrumentation;

/**
 * An abstraction for logging. This makes it easier to connect the Instrumentor
 * to various logging systems that can be determined by applications using the
 * Instrumentor api.
 *
 * @author Peter Veentjer
 */
public interface InstrumenterLogger {

    void important(String msg, Object... args);

    void lessImportant(String msg, Object... args);
}
