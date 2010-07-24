package org.multiverse.stms.beta;

/**
 * @author Peter Veentjer
 */
public class ThreadLocalObjectPool {

    public final static ThreadLocal<ObjectPool> threadlocal = new ThreadLocal<ObjectPool>(){
        @Override
        protected ObjectPool initialValue() {
            return new ObjectPool();
        }
    };

     public static ObjectPool getThreadLocalObjectPool() {
        return threadlocal.get();
    }

    //we don't want any instances.
    private ThreadLocalObjectPool() {
    }
}
