package org.multiverse.stms.beta;

/**
 *
 * @author Peter Veentjer.
 */
public class ThreadLocalBetaObjectPool {

    public final static ThreadLocal<BetaObjectPool> threadlocal = new ThreadLocal<BetaObjectPool>() {
        protected BetaObjectPool initialValue() {
            return new BetaObjectPool();
        }
    };

    public static BetaObjectPool getThreadLocalBetaObjectPool() {
        return threadlocal.get();
    }

    private ThreadLocalBetaObjectPool() {
    }
}
