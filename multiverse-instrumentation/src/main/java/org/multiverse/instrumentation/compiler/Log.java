package org.multiverse.instrumentation.compiler;

/**
 * An abstraction for logging. This makes it easier to connect the ClazzCompiler
 * to various logging systems that can be determined by applications using the
 * ClazzCompiler api.
 *
 * @author Peter Veentjer
 */
public interface Log {

    void important(String msg, Object... args);

    void lessImportant(String msg, Object... args);
}
