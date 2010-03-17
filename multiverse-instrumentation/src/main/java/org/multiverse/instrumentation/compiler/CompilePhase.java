package org.multiverse.instrumentation.compiler;

/**
 * @author Peter Veentjer
 */
public interface CompilePhase {

    String getName();

    /**
     * @param environment
     * @param clazz
     * @return
     * @throws CompileException if something fails while doing the processing.
     */
    Clazz compile(Environment environment, Clazz clazz);
}
