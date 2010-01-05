package org.multiverse.utils.clock;

/**
 * A {@link Clock} implementation that can be used in a single thread environment. This is
 * useful if you want to use the atomic behavior of the stm but don't need it to be threadsafe.
 *
 * Another purpose of this Clock is for benchmarking; to see what to overhead of an AtomicLong
 * in a single threaded environment is.
 *
 * @author Peter Veentjer.
 */
public class SingleThreadedClock implements Clock{

    private long time = 0;

    @Override
    public long tick() {
        time++;
        return time;
    }

    @Override
    public long getVersion() {
        return time;
    }
}
