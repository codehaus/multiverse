package org.multiverse.stms.gamma;

import org.multiverse.stms.gamma.transactionalobjects.GammaObject;

public class GammaStmUtils {
    public static String toDebugString(GammaObject o) {
        if (o == null) {
            return "null";
        } else {
            return o.getClass().getName() + '@' + System.identityHashCode(o);
        }
    }
}
