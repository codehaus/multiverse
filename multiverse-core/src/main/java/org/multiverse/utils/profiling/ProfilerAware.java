package org.multiverse.utils.profiling;

/**
 * The Object being profiled. It can be added to an STM implementation for example to
 * indicate that it has a profiler so this can be used to retrieve the information.
 * <p/>
 * This interface is introduced to prevent adding a dependency on the profiler package
 * in the stm implementations.
 *
 * @author Peter Veentjer.
 */
public interface ProfilerAware {

    /**
     * Returns the Profiler used. If null is returned, no profiler is used.
     *
     * @return the Profiler used (can be null).
     */
    ProfileRepository getProfiler();
}
