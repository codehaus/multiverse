package org.multiverse.utils.clock;

import java.util.concurrent.atomic.AtomicLong;

import static java.lang.String.format;

/**
 * The intuitive implementation of a {@link Clock}. It wraps an AtomicLong and increases the value every time a tick is
 * done.
 * <p/>
 * A strict clock provides a full ordering of all transactions (also transactions that don't share state). This full
 * ordering causes contention on the memory bus. See the {@link RelaxedClock} for more info.
 *
 * @author Peter Veentjer.
 */
public final class StrictClock implements Clock {

    private final AtomicLong clock = new AtomicLong();

    /**
     * Creates a new StrictClock.
     */
    public StrictClock() {
    }

    public StrictClock(long time) {
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
        return format("StrictClock(time=%s)", clock.get());
    }
}

