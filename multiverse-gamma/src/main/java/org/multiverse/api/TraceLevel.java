package org.multiverse.api;

/**
 * Using the TraceLevel you get some feedback on what is happening inside a transaction.
 *
 * <p>For tracing to work, you need to look at {@link org.multiverse.MultiverseConstants#TRACING_ENABLED}. If not enabled,
 * the JIT will remove dead code because we don't want any overhead.
 *
 * @author Peter Veentjer
 * @see TransactionFactoryBuilder#setTraceLevel(TraceLevel)
 * @see TransactionConfiguration#getTraceLevel()
 */
public enum TraceLevel {

    None, Course;

    /**
     * Checks if the provided level is higher than this TraceLevel.
     *
     * @param level the TraceLevel to check
     * @return true if level is higher or equal than this TraceLevel.
     * @throws NullPointerException if level is null.
     */
    public boolean isLoggableFrom(TraceLevel level) {
        if(level == null){
            throw new NullPointerException();
        }
        return compareTo(level) >= 0;
    }
}
