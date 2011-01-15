package org.multiverse.stms.gamma.transactionalobjects;

import org.multiverse.stms.gamma.GammaStm;

public abstract class AbstractGammaRef extends AbstractGammaObject {

    private final int type;

    protected AbstractGammaRef(GammaStm stm, int type) {
        super(stm);
        this.type = type;
    }

    public int getType(){
        return type;
    }
}
