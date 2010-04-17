package org.multiverse.instrumentation;

/**
 * An Annotation Multiverse places on class-files that have been instrumented. This is needed
 * to prevent reinstrumentation or to detect that classes can't be used with this specific
 * instrumentor or version of that instrumentor.
 *
 * @author Peter Veentjer
 */
public @interface InstrumentationStamp {

    /**
     * The name of the Instrumentor.
     *
     * @return the name of the Instrumentor.
     */
    String instrumentorName();

    /**
     * The version of the Instrumentor.
     *
     * @return the version of the Instrumentor.
     */
    String instrumentorVersion();
}
