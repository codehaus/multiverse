package org.multiverse.stms.beta;

/**
 * @author Peter Veentjer
 */
public abstract class BetaTranlocal {

    public long ___version;

    public abstract BetaTranlocal cloneForUpdate();

    public abstract BetaTranlocalStatus getStatus();
}
