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
 * It is an interface so that is can be implemented for easier access.
 *
 * @author Peter Veentjer
 */
public interface MultiverseConstants {


    boolean BUGSHAKER_ENABLED =
            parseBoolean(getProperty(MultiverseConstants.class.getName() + ".bugshaker", "true"));


    /**
     * A flag that enables the sanity check build in the system.
     * <p/>
     * If sanity checks are enabled, the system can check all kinds of design by contract violations using pre/post
     * condition checks and checking the invariants. It could be that these checks are costly so as end used you want to
     * turn this off. For the time being it is true by default.
     * <p/>
     * <p/>
     * If the sanity checks are disabled, the JIT is completely removing
     */
    boolean SANITY_CHECKS_ENABLED =
            parseBoolean(getProperty(MultiverseConstants.class.getName() + ".sanityChecks", "false"));
}
