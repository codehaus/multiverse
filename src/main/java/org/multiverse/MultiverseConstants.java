package org.multiverse;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;

/**
 * An interface containing global constants (currently only sanity check). It is a final instead of something mutable so
 * that the JIT can completely remove code if some condition has not been met. The advantage is that you don't have to
 * pay to price for adding some kind of check, if it isn't used. The problem is that the scope is all classes loaded by
 * some classloader, share the same configuration. So one STM implementation with sanity checks enabled and the other
 * not, is not possible.
 * <p/>
 * It is an interface so that is can be 'implemented' for easier access.
 *
 * @author Peter Veentjer
 */
public interface MultiverseConstants {

    boolean ___BugshakerEnabled =
            parseBoolean(getProperty("org.multiverse.bugshaker.enabled", "false"));

    boolean ___TracingEnabled =
            parseBoolean(getProperty("org.multiverse.tracing.enabled", "false"));

    boolean ___ProfilingEnabled =
            parseBoolean(getProperty("org.multiverse.profiling.enabled", "false"));
}
