package org.multiverse.stms.beta.transactionalobjects;

import org.multiverse.api.functions.LongFunction;

public class IdentityLongFunction extends LongFunction {
    @Override
    public long call(long current) {
        return current;
    }
}
