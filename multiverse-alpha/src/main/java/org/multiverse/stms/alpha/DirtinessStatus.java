package org.multiverse.stms.alpha;

/**
 * A {@link AlphaTranlocal} can have different 'dirtiness' states.
 *
 * @author Peter Veentjer.
 */
public enum DirtinessStatus {

    fresh, dirty, clean, readonly, conflict
}
