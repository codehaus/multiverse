package org.multiverse.api;

/**
 * @author Peter Veentjer
 */
public enum LogLevel {

    none, course, fine;

    public boolean isLogableFrom(LogLevel level) {
        return compareTo(level) >= 0;
    }
}
