package org.multiverse.stms.beta;

import org.multiverse.MultiverseConstants;

/**
 * Contains various constants for the BetaStm.
 *
 * @author Peter Veentjer.
 */
public interface BetaStmConstants extends MultiverseConstants {

    int REGISTRATION_DONE = 0;
    int REGISTRATION_NOT_NEEDED = 1;
    int REGISTRATION_NONE = 2;

    int DIRTY_FALSE = 0;
    int DIRTY_TRUE = 1;
    int DIRTY_UNKNOWN = 2;
}
