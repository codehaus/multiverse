package org.multiverse.instrumentation.metadata;

/**
 * Indicates what type of method it is. Based on this information the system is able to
 * allow certain optimizations.
 *
 * @author Peter Veentjer
 */
public enum MethodType {
    unknown, getter, setter
}
