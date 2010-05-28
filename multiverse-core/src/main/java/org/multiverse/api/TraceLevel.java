package org.multiverse.api;

/**
 * @author Peter Veentjer
 */
public enum TraceLevel {

    none, course, fine;

    public boolean isLogableFrom(TraceLevel level) {
        return compareTo(level) >= 0;
    }
}
