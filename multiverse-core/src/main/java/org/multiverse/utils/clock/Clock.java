package org.multiverse.utils.clock;

/**
 * A Clock: a logical timer. It is to make a sense of time possible so that concurrent transactions that commit are
 * ordered. Unlike normal clocks that change based on linear time, a logical clock is non linear; sometimes there are
 * bursts and some there is not much to do. Time is increased by calling {@link #tick}
 * <p/>
 * Concurrent transactions are only interesting when there is shared state. Transactions that don't share state are not
 * ordered. So the relation partially orders transactions in time. Only transactions that share state are fully ordered.
 * It they weren't, the system would start to suffer from isolation problems: <ol> <li> transactions seeing state of
 * other transactions that completed before it did. </li> <li> a transaction not seeing state of other transactions that
 * started after the transaction but completed before (in databases this is called the lost update problem). </li>
 * </ol>
 * <p/>
 * <h3>Small Warning</h3> The Clock is based on a long, so there is a limit on the total time a clock is able to live.
 * If a clock lives longer, the time could go 'back' when the long flows over to the negative side. In most cases this
 * won't cause problems; it the dawn time is zero and there are 1.000.0000.0000 transactions per second, only after
 * Long.MAX_VALUE/1.000.0000.000 transactions per second, the clock is able to run for 9223372036 second; which is
 * roughly 292 years.
 * <p/>
 * All clocks always start from 0. An stm could give a clock extra ticks when it starts, so that some versions for the
 * time can be (miss)used to describe some state.
 *
 * @author Peter Veentjer.
 */
public interface Clock {

    /**
     * Executes a clock tick by increasing the version. The returned value could be stale as soon as it is received. The
     * returned value will always be bigger than the current version. Once the tick method completes, the {@link
     * #getVersion()} method will always return a time equal or larger than.the last tick.
     *
     * @return the time after the tick.
     */
    long tick();

    /**
     * Returns the current version. The returned value could be stale as soon as it is received.
     *
     * @return the current version.
     */
    long getVersion();

}

