package org.multiverse.instrumentation;

/**
 * The compilation process can be split up in different steps; the InstrumentationPhase is one
 * such step.
 *
 * @author Peter Veentjer
 */
public interface InstrumentationPhase {

    /**
     * Returns the name for this InstrumentationPhase. It is only used for logging purposes.
     *
     * @return the name of this InstrumentationPhase.
     */
    String getName();

    /**
     * Compiles (transforms) the originalClazz. If nothing is changed, the originalClazz
     * can be returned. The returned value never should be null.
     *
     * @param environment
     * @param originalClazz the originalClazz.
     * @return the compiled clazz. Null means that following compile phases should
     *         be skipped. This makes it possible to add control flow compile phases
     *         (for example if the class already has been
     * @throws org.multiverse.instrumentation.CompileException
     *          if something fails while doing the processing.
     */
    Clazz instrument(Environment environment, Clazz originalClazz);
}
