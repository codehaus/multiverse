package org.multiverse.stms.beta.transactionalobjects;

import org.multiverse.api.references.BooleanRef;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

public class BetaBooleanRef extends BetaIntRef implements BooleanRef {

    public BetaBooleanRef(BetaTransaction tx) {
        super(tx);
    }

    public BetaBooleanRef(BetaStm stm) {
        super(stm);
    }

    public BetaBooleanRef(BetaStm stm, int initialValue) {
        super(stm, initialValue);
    }
}
