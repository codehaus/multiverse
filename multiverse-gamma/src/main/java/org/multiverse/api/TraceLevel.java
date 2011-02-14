package org.multiverse.api;

/**
 * Using the TraceLevel you get some feedback on what is happening inside a transaction.
 *
 * For tracing to work, you need to look at {@link org.multiverse.MultiverseConstants#TRACING_ENABLED}. If not enabled,
 * the JIT will remove dead code because we don't want any overhead.
 *
 * @author Peter Veentjer
 * @see TransactionFactoryBuilder#setTraceLevel(TraceLevel)
 * @see TransactionConfiguration#getTraceLevel()
 */
public enum TraceLevel {

    None, Course;

    public boolean isLoggableFrom(TraceLevel level) {
        return compareTo(level) >= 0;
    }
}
