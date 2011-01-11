package org.multiverse.stms.gamma;

public class ThreadLocalGammaObjectPool {
        public final static ThreadLocal<GammaObjectPool> threadlocal = new ThreadLocal<GammaObjectPool>() {
        protected GammaObjectPool initialValue() {
            return new GammaObjectPool();
        }
    };

    public static GammaObjectPool getThreadLocalGammaObjectPool() {
        return threadlocal.get();
    }

    private ThreadLocalGammaObjectPool() {
    }
}
