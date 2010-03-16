package org.multiverse.instrumentation.compiler;

/**
 * @author Peter Veentjer
 */
public interface CompileStep {

    /**
     * @param environment
     * @param clazz
     * @return
     * @throws ProcessException if something fails while doing the processing.
     */
    Clazz transform(Environment environment, Clazz clazz);
}
