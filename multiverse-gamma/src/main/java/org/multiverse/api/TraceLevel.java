package org.multiverse.api;

/**
 * Using the TraceLevel you get some feedback on what is happening inside a transaction.
 *
 * @author Peter Veentjer
 */
public enum TraceLevel {

    None, Course;

    public boolean isLogableFrom(TraceLevel level) {
        return compareTo(level) >= 0;
    }
}
