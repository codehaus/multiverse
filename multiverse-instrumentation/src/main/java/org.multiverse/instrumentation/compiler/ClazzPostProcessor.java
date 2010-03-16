package org.multiverse.instrumentation.compiler;

/**
 * The ClazzPostProcessor is responsible for doing static instrumentation (compiletime weaving) instead of dynamic instrumentation
 * (load time weaving).
 * <p/>
 * todo: could the same mechanism be used for the compiletime and dynamic stuff?
 *
 * @author Peter Veentjer
 */
public interface ClazzPostProcessor {

    /**
     * Processes a clazz. If nothing needs to be processed, the provided clazz can be returned. The return
     * value should never be null.
     *
     * @param clazz the Clazz to transform.
     * @return the transformed clazz. It is an array because additional new classes can be generated.
     * @throws ProcessException
     */
    Clazz process(Clazz clazz);
}
