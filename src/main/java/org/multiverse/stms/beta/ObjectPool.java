package org.multiverse.stms.beta;

import org.multiverse.api.blocking.*;
import org.multiverse.api.exceptions.*;
import org.multiverse.stms.beta.refs.*;

/**
 * A pool for tranlocals. The pool is not threadsafe and should be connected to a thread (can
 * be stored in a threadlocal). Eventually the performance of the stm will be limited to the rate
 * of cleanup, and using a pool seriously improves scalability.
 * <p/>
 * Improvement: atm there is only one single type of tranlocal. If there are more types of tranlocals,
 * each class needs to have an index. This index can be used to determine the type of ref. If the pool
 * contains an array of arrays, where the first array is index based on the type of the ref, finding the
 * second array (that contains pooled tranlocals) can be found easily.
 * <p/>
 * ObjectPool is not thread safe and should not be shared between threads.
 *
 * This class is generated.
 *
 * @author Peter Veentjer
 */
public final class ObjectPool {


    private final boolean enabled;
    private final boolean arrayPoolingEnabled;
    private final boolean latchPoolingEnabled;
    private final boolean listenersPoolingEnabled;
    private final RefTranlocal[] tranlocalsRef = new RefTranlocal[100];
    private int lastUsedRef = -1;
    private final IntRefTranlocal[] tranlocalsIntRef = new IntRefTranlocal[100];
    private int lastUsedIntRef = -1;
    private final LongRefTranlocal[] tranlocalsLongRef = new LongRefTranlocal[100];
    private int lastUsedLongRef = -1;
    private final DoubleRefTranlocal[] tranlocalsDoubleRef = new DoubleRefTranlocal[100];
    private int lastUsedDoubleRef = -1;
    private TranlocalPool[] pools = new TranlocalPool[1000];
    private CheapLatch[] cheapLatchPool = new CheapLatch[10];
    private int cheapLatchPoolIndex = -1;
    private Listeners[] listenersPool = new Listeners[1000];
    private int listenersPoolIndex = -1;

    public ObjectPool() {
        enabled = true;
        arrayPoolingEnabled = true;
        latchPoolingEnabled = true;
        listenersPoolingEnabled = true;
    }

    /**
     * Takes a RefTranlocal from the pool for the specified Ref.
     *
     * @param owner the Ref to get the RefTranlocal for.     
     * @return the pooled tranlocal, or null if none is found.
     * @throws NullPointerException if owner is null.
     */
    public RefTranlocal take(final Ref owner) {
        if (owner == null) {
            throw new NullPointerException();
        }

        if (!enabled) {
            return null;
        }

        if (lastUsedRef == -1) {
            return null;
        }

        RefTranlocal tranlocal = tranlocalsRef[lastUsedRef];
        tranlocal.owner = owner;
        tranlocalsRef[lastUsedRef] = null;
        lastUsedRef--;
        return tranlocal;
    }

    /**
     * Puts an old RefTranlocal in this pool. If the tranlocal is allowed to be null,
     * the call is ignored. The same goes for when the tranlocal is permanent, since you
     * can't now how many transactions are still using it.
     *
     * @param tranlocal the RefTranlocal to pool.
     */
    public void put(final RefTranlocal tranlocal) {
        if (!enabled || tranlocal == null||tranlocal.isPermanent) {
            return;
        }

        if (lastUsedRef == tranlocalsRef.length - 1) {
            return;
        }

        tranlocal.clean();
        lastUsedRef++;
        tranlocalsRef[lastUsedRef] = tranlocal;
    }

    /**
     * Takes a IntRefTranlocal from the pool for the specified IntRef.
     *
     * @param owner the IntRef to get the IntRefTranlocal for.     
     * @return the pooled tranlocal, or null if none is found.
     * @throws NullPointerException if owner is null.
     */
    public IntRefTranlocal take(final IntRef owner) {
        if (owner == null) {
            throw new NullPointerException();
        }

        if (!enabled) {
            return null;
        }

        if (lastUsedIntRef == -1) {
            return null;
        }

        IntRefTranlocal tranlocal = tranlocalsIntRef[lastUsedIntRef];
        tranlocal.owner = owner;
        tranlocalsIntRef[lastUsedIntRef] = null;
        lastUsedIntRef--;
        return tranlocal;
    }

    /**
     * Puts an old IntRefTranlocal in this pool. If the tranlocal is allowed to be null,
     * the call is ignored. The same goes for when the tranlocal is permanent, since you
     * can't now how many transactions are still using it.
     *
     * @param tranlocal the IntRefTranlocal to pool.
     */
    public void put(final IntRefTranlocal tranlocal) {
        if (!enabled || tranlocal == null||tranlocal.isPermanent) {
            return;
        }

        if (lastUsedIntRef == tranlocalsIntRef.length - 1) {
            return;
        }

        tranlocal.clean();
        lastUsedIntRef++;
        tranlocalsIntRef[lastUsedIntRef] = tranlocal;
    }

    /**
     * Takes a LongRefTranlocal from the pool for the specified LongRef.
     *
     * @param owner the LongRef to get the LongRefTranlocal for.     
     * @return the pooled tranlocal, or null if none is found.
     * @throws NullPointerException if owner is null.
     */
    public LongRefTranlocal take(final LongRef owner) {
        if (owner == null) {
            throw new NullPointerException();
        }

        if (!enabled) {
            return null;
        }

        if (lastUsedLongRef == -1) {
            return null;
        }

        LongRefTranlocal tranlocal = tranlocalsLongRef[lastUsedLongRef];
        tranlocal.owner = owner;
        tranlocalsLongRef[lastUsedLongRef] = null;
        lastUsedLongRef--;
        return tranlocal;
    }

    /**
     * Puts an old LongRefTranlocal in this pool. If the tranlocal is allowed to be null,
     * the call is ignored. The same goes for when the tranlocal is permanent, since you
     * can't now how many transactions are still using it.
     *
     * @param tranlocal the LongRefTranlocal to pool.
     */
    public void put(final LongRefTranlocal tranlocal) {
        if (!enabled || tranlocal == null||tranlocal.isPermanent) {
            return;
        }

        if (lastUsedLongRef == tranlocalsLongRef.length - 1) {
            return;
        }

        tranlocal.clean();
        lastUsedLongRef++;
        tranlocalsLongRef[lastUsedLongRef] = tranlocal;
    }

    /**
     * Takes a DoubleRefTranlocal from the pool for the specified DoubleRef.
     *
     * @param owner the DoubleRef to get the DoubleRefTranlocal for.     
     * @return the pooled tranlocal, or null if none is found.
     * @throws NullPointerException if owner is null.
     */
    public DoubleRefTranlocal take(final DoubleRef owner) {
        if (owner == null) {
            throw new NullPointerException();
        }

        if (!enabled) {
            return null;
        }

        if (lastUsedDoubleRef == -1) {
            return null;
        }

        DoubleRefTranlocal tranlocal = tranlocalsDoubleRef[lastUsedDoubleRef];
        tranlocal.owner = owner;
        tranlocalsDoubleRef[lastUsedDoubleRef] = null;
        lastUsedDoubleRef--;
        return tranlocal;
    }

    /**
     * Puts an old DoubleRefTranlocal in this pool. If the tranlocal is allowed to be null,
     * the call is ignored. The same goes for when the tranlocal is permanent, since you
     * can't now how many transactions are still using it.
     *
     * @param tranlocal the DoubleRefTranlocal to pool.
     */
    public void put(final DoubleRefTranlocal tranlocal) {
        if (!enabled || tranlocal == null||tranlocal.isPermanent) {
            return;
        }

        if (lastUsedDoubleRef == tranlocalsDoubleRef.length - 1) {
            return;
        }

        tranlocal.clean();
        lastUsedDoubleRef++;
        tranlocalsDoubleRef[lastUsedDoubleRef] = tranlocal;
    }

    public Tranlocal take(final BetaTransactionalObject owner) {
        if (owner == null) {
            throw new NullPointerException();
        }

        if(!enabled){
            return null;
        }

        int classIndex = owner.getClassIndex();

        switch(classIndex){
            case 0:
                return take((Ref)owner);
            case 1:
                return take((IntRef)owner);
            case 2:
                return take((LongRef)owner);
            case 3:
                return take((DoubleRef)owner);
        }

        if(classIndex >= pools.length){
            return null;
        }

        TranlocalPool pool = pools[classIndex];
        if(pool.lastUsed == -1){
            return null;
        }

        Tranlocal tranlocal = pool.tranlocals[pool.lastUsed];
        tranlocal.owner = owner;
        pool.tranlocals[pool.lastUsed] = null;
        pool.lastUsed--;
        return tranlocal;
    }

    public void put(final Tranlocal tranlocal) {
        if (!enabled || tranlocal == null || tranlocal.isPermanent) {
            return;
        }

        BetaTransactionalObject owner = tranlocal.owner;
        int classIndex = owner.getClassIndex();

        switch(classIndex){
            case 0:
                put((RefTranlocal)tranlocal);
                return;
            case 1:
                put((IntRefTranlocal)tranlocal);
                return;
            case 2:
                put((LongRefTranlocal)tranlocal);
                return;
            case 3:
                put((DoubleRefTranlocal)tranlocal);
                return;
        }

        if(classIndex >= pools.length){
            TranlocalPool[] newPools = new TranlocalPool[pools.length * 2];
            System.arraycopy(pools, 0, newPools, 0, pools.length);
            pools = newPools;
        }

        TranlocalPool pool = pools[classIndex];
        if(pool == null){
            pool = new TranlocalPool();
            pools[classIndex]=pool;
        }

        if(pool.lastUsed == pool.tranlocals.length - 1){
            return;
        }

        tranlocal.clean();
        pool.lastUsed++;
        pool.tranlocals[pool.lastUsed] = tranlocal;
    }

    static class TranlocalPool{
        int lastUsed = -1;
        Tranlocal[] tranlocals = new Tranlocal[100];
    }

    private Tranlocal[][] tranlocalArrayPool = new Tranlocal[8193][];

    public void putTranlocalArray(final Tranlocal[] array){
        if(array == null){
            throw new NullPointerException();
        }

        if(!arrayPoolingEnabled){
            return;
        }

        if(array.length-1>tranlocalArrayPool.length){
            return;
        }

        int index = array.length;

        if(tranlocalArrayPool[index]!=null){
            return;
        }

        //lets clean the array
        for(int k=0;k<array.length;k++){
            array[k]=null;
        }

        tranlocalArrayPool[index]=array;
    }

    /**
     * Takes a tranlocal array from the pool
     */
    public Tranlocal[] takeTranlocalArray(final int size){
        if(size<0){
            throw new IllegalArgumentException();
        }

        if(!arrayPoolingEnabled){
            return null;
        }

        int index = size;

        if(index>=tranlocalArrayPool.length){
            return null;
        }

        if(tranlocalArrayPool[index]==null){
            return null;
        }

        Tranlocal[] array = tranlocalArrayPool[index];
        tranlocalArrayPool[index]=null;
        return array;
    }

    public CheapLatch takeCheapLatch(){
        if(!latchPoolingEnabled || cheapLatchPoolIndex == -1){
            return null;
        }

        CheapLatch latch = cheapLatchPool[cheapLatchPoolIndex];
        cheapLatchPool[cheapLatchPoolIndex]=null;
        cheapLatchPoolIndex--;
        return latch;
    }

    public void putCheapLatch(CheapLatch latch){
        if(latch == null){
            throw new NullPointerException();
        }

        if(!latchPoolingEnabled || cheapLatchPoolIndex == cheapLatchPool.length-1){
            return;
        }

        latch.reset();
        cheapLatchPoolIndex++;
        cheapLatchPool[cheapLatchPoolIndex]=latch;        
    }

    public Listeners takeListeners(){
        if(!listenersPoolingEnabled || listenersPoolIndex == -1){
            return null;
        }

        Listeners listeners = listenersPool[listenersPoolIndex];
        listenersPool[listenersPoolIndex]=null;
        listenersPoolIndex--;
        return listeners;
    }

    public void putListeners(Listeners listeners){
        if(listeners == null){
            throw new NullPointerException();
        }

        if(!listenersPoolingEnabled || listenersPoolIndex == listenersPool.length-1){
            return;
        }

        listeners.reset();
        listenersPoolIndex++;
        listenersPool[listenersPoolIndex]=listeners;
    }
}
