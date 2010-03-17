package org.multiverse.instrumentation.compiler;

/**
 * The compilation process can be split up in different steps; the CompilePhase is one
 * such step.
 *
 * @author Peter Veentjer
 */
public interface CompilePhase {

    /**
     * Returns the name for this CompilePhase. It is only used for logging purposes.
     *
     * @return the name of this CompilePhase.
     */
    String getName();

    /**
     * Compiles (transforms) the originalClazz. If nothing is changed, the originalClazz
     * can be returned. The returned value never should be null.
     *
     * @param environment
     * @param originalClazz the originalClazz.
     * @return the compiled clazz.
     * @throws CompileException if something fails while doing the processing.
     */
    Clazz compile(Environment environment, Clazz originalClazz);
}
