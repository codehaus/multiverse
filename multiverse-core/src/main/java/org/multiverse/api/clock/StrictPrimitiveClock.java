package org.multiverse.api.clock;

import java.util.concurrent.atomic.AtomicLong;

import static java.lang.String.format;

/**
 * The intuitive implementation of a {@link PrimitiveClock}. It wraps an AtomicLong and increases the value every
 * time a tick is done.
 * <p/>
 * A strict clock provides a full ordering of all transactions (also transactions that don't share state). This full
 * ordering can cause a lot of contention on the memory bus. See the {@link RelaxedPrimitiveClock} for more info.
 * <p/>
 * A StrictPrimitiveClock is thread-safe.
 * <p/>
 * Reading the version is very cheap (even though a volatile read needs to be executed).
 *
 * @author Peter Veentjer.
 */
public final class StrictPrimitiveClock implements PrimitiveClock {

    private final AtomicLong clock = new AtomicLong();

    /**
     * Creates a new StrictPrimitiveClock.
     */
    public StrictPrimitiveClock() {
    }

    public StrictPrimitiveClock(long time) {
        if (time < 0) {
            throw new IllegalArgumentException();
        }
        clock.set(time);
    }

    @Override
    public long strictTick() {
        return tick();
    }

    @Override
    public long tick() {
        return clock.incrementAndGet();
    }

    @Override
    public long getVersion() {
        return clock.get();
    }

    @Override
    public String toString() {
        return format("StrictPrimitiveClock(time=%s)", clock.get());
    }
}

