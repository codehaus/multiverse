package org.multiverse.utils.clock;

import static java.lang.String.format;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The RelaxedClock is less strict about increasing the clock. It finds it ok if someone has increased the clock instead
 * of it encysting that it wants to increase the clock as well. Synchronization should prevent transactions sharing
 * state to execute concurrently, so only independent transaction increase the clock.
 * <p/>
 * The reason why this implementation exists, is that it causes less stress on the memory bus because compare and swaps
 * are done.
 * <p/>
 * The RelaxedClock is a first and simple step forwards to prevent contention on the memory bus. The ideal situation
 * would be if the stm didn't rely on a shared clock. The experimental STM implementation for the .NET platform already
 * has realized this.
 * <p/>
 * <h3>Warning</h3> A relaxed clock can not be used as a mechanism to find the total number of committed transactions.
 * Because concurrent executing transaction don't both have to increase the clock; as long as one of them does it
 * suffices as well.
 *
 * @author Peter Veentjer.
 */
public final class RelaxedClock implements Clock {

    private final AtomicLong clock;

    /**
     * Creates a RelaxedClock.
     */
    public RelaxedClock() {
        this.clock = new AtomicLong(0);
    }

    @Override
    public long tick() {
        long oldTime = clock.get();
        //it doesn't matter if we increase it, or if someone else increases it.
        //so just try to set it the oldTime is still set.
        clock.compareAndSet(oldTime, oldTime + 1);

        //todo: could it be that time on the clock hasn't increased yet?
        return oldTime + 1;
    }

    @Override
    public long getVersion() {
        return clock.get();
    }

    @Override
    public String toString() {
        return format("RelaxedClock(time=%s)", clock.get());
    }
}


