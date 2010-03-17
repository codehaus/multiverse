package org.multiverse.instrumentation.compiler;

/**
 * The Filer is responsible for creating class definitions. It is used as a callback mechanism
 * so other tools that would like to integrate with the
 *
 * @author Peter Veentjer
 */
public interface Filer {

    void createClassFile(Clazz clazz);
}
