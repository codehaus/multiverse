package org.multiverse.stms.beta;

/**
 * A {@link org.multiverse.stms.alpha.AlphaTranlocal} can have different 'dirtiness' states.
 *
 * @author Peter Veentjer.
 */
public enum DirtynessStatus {

    fresh, dirty, clean, readonly, conflict
}
