package org.multiverse.instrumentation.metadata;

/**
 * @author Peter Veentjer
 */
public interface FamilyNameStrategy {

    String create(String className, String methodName, String desc);
}
