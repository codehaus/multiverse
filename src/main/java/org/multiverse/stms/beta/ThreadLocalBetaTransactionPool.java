package org.multiverse.stms.beta;

/**
 * A ThreadLocal containing the BetaObjectPool. BetaObjectPool is not threadsafe and storing it in a threadlocal
 * is the safest way to get a reference to it.
 *
 * @author Peter Veentjer
 */
public class ThreadLocalBetaTransactionPool {

    private final static ThreadLocal<BetaTransactionPool> threadlocal = new ThreadLocal<BetaTransactionPool>() {
        @Override
        protected BetaTransactionPool initialValue() {
            return new BetaTransactionPool();
        }
    };


    /**
     * Returns the BetaObjectPool stored in the ThreadLocalBetaTransactionPool. If no instance exists,
     * a new instance is created.
     *
     * @return the BetaObjectPool.
     */
    public static BetaTransactionPool getThreadLocalBetaTransactionPool() {
        return threadlocal.get();
    }

    //we don't want any instances.

    private ThreadLocalBetaTransactionPool() {
    }
}
