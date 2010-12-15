package org.multiverse.api;

/**
 * @author Peter Veentjer
 */
public enum TraceLevel {

    None, Course, Fine;

    public boolean isLogableFrom(TraceLevel level) {
        return compareTo(level) >= 0;
    }
}
