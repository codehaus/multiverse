package org.multiverse.utils.clock;

import static java.lang.String.format;

/**
 * A {@link PrimitiveClock} implementation that can be used in a single thread environment. It is useful if you want
 * to use the atomic behavior of the stm but don't need it to be thread-safe.
 * <p/>
 * Another purpose of this PrimitiveClock is for benchmarking; to see what to overhead of an AtomicLong
 * in a single threaded environment is.
 *
 * @author Peter Veentjer.
 */
public final class SingleThreadedPrimitiveClock implements PrimitiveClock {

    private long time = 0;

    @Override
    public long tick() {
        time++;
        return time;
    }

    @Override
    public long strictTick() {
        return tick();
    }

    @Override
    public long getVersion() {
        return time;
    }

    @Override
    public String toString() {
        return format("SingleThreadedPrimitiveClock(time=%s)", time);
    }
}
