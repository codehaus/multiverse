package org.multiverse.stms.beta;

/**
 * A ThreadLocal containing the BetaObjectPool. BetaObjectPool is not threadsafe and storing it in a threadlocal
 * is the safest way to get a reference to it.
 *
 * @author Peter Veentjer
 */
public class ThreadLocalBetaObjectPool {

    private final static ThreadLocal<BetaObjectPool> threadlocal = new ThreadLocal<BetaObjectPool>() {
        @Override
        protected BetaObjectPool initialValue() {
            return new BetaObjectPool();
        }
    };


    /**
     * Returns the BetaObjectPool stored in the ThreadLocalBetaObjectPool. If no instance exists,
     * a new instance is created.
     *
     * @return the BetaObjectPool.
     */
    public static BetaObjectPool getThreadLocalBetaObjectPool() {
        return threadlocal.get();
    }

    //we don't want any instances.

    private ThreadLocalBetaObjectPool() {
    }
}
