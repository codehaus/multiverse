package org.multiverse.instrumentation;

/**
 * @author Peter Veentjer
 */
public @interface Instrumented {

    String instrumentorName();

    String instrumentorVersion();
}
