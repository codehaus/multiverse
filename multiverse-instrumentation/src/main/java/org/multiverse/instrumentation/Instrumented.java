package org.multiverse.instrumentation;

/**
 * Annotation Multiverse placed on classfiles that have been instrumented. Needed
 * to prevent reinstrumentation or to detect that classes can't be used with this
 * specific Instrumentation/version.
 *
 * @author Peter Veentjer
 */
public @interface Instrumented {

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
