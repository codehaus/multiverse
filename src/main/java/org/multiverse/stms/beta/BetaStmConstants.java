package org.multiverse.stms.beta;

import org.multiverse.MultiverseConstants;

/**
 * Contains various constants for the BetaStm.
 *
 * @author Peter Veentjer.
 */
public interface BetaStmConstants extends MultiverseConstants {

    int ARRIVE_NORMAL = 0;
    int ARRIVE_UNREGISTERED = 1;
    int ARRIVE_LOCK_NOT_FREE = 2;

    int REGISTRATION_DONE = 0;
    int REGISTRATION_NOT_NEEDED = 1;
    int REGISTRATION_NONE = 2;

    int STATUS_NEW = 0;
    int STATUS_CONSTRUCTING = 1;
    int STATUS_UPDATE = 2;
    int STATUS_COMMUTING = 3;
    int STATUS_READONLY = 4;
}
