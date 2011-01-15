package org.multiverse.stms.gamma;

import org.multiverse.MultiverseConstants;

public interface GammaConstants extends MultiverseConstants {

    int ARRIVE_NORMAL = 0;
    int ARRIVE_UNREGISTERED = 1;
    int ARRIVE_LOCK_NOT_FREE = 2;

    int REGISTRATION_DONE = 0;
    int REGISTRATION_NOT_NEEDED = 1;
    int REGISTRATION_NONE = 2;

    int TRANLOCAL_UNUSED = 0;
    int TRANLOCAL_CONSTRUCTING = 1;
    int TRANLOCAL_WRITE = 2;
    int TRANLOCAL_COMMUTING = 3;
    int TRANLOCAL_READ = 4;

    int TX_ACTIVE = 1;
    int TX_PREPARED = 2;
    int TX_ABORTED = 3;
    int TX_COMMITTED = 4;

    int TYPE_LONG = 1;
    int TYPE_DOUBLE = 2;
    int TYPE_REF = 3;
}
