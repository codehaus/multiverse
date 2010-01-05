package org.multiverse.stms.beta;

/**
 * @author Peter Veentjer
 */
public interface CommuteTask {

    DelayedBetaAtomicObject getAtomicObject();

    void run(BetaTransaction t);
}
