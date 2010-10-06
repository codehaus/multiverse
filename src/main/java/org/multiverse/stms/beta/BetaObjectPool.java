package org.multiverse.stms.beta;

import org.multiverse.api.blocking.CheapLatch;
import org.multiverse.api.blocking.StandardLatch;
import org.multiverse.stms.beta.transactionalobjects.*;

import java.util.ArrayList;

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
public final class BetaObjectPool {

    private final static boolean ENABLED = Boolean.parseBoolean(
        System.getProperty("org.multiverse.stm,beta.BetaObjectPool.enabled","true"));

    private final static boolean TRANLOCAL_POOLING_ENABLED = Boolean.parseBoolean(
        System.getProperty("org.multiverse.stm.beta.BetaObjectPool.tranlocalPooling",""+ENABLED));

    private final static boolean TRANLOCALARRAY_POOLING_ENABLED = Boolean.parseBoolean(
        System.getProperty("org.multiverse.stm.beta.BetaObjectPool.tranlocalArrayPooling",""+ENABLED));

    private final static boolean LATCH_POOLING_ENABLED = Boolean.parseBoolean(
        System.getProperty("org.multiverse.stm.beta.BetaObjectPool.latchPooling",""+ENABLED));

    private final static boolean LISTENER_POOLING_ENABLED  = Boolean.parseBoolean(
        System.getProperty("org.multiverse.stm.beta.BetaObjectPool.listenersPooling",""+ENABLED));

    private final static boolean LISTENERSARRAY_POOLING_ENABLED  = Boolean.parseBoolean(
        System.getProperty("org.multiverse.stm.beta.BetaObjectPool.listenersArrayPooling",""+ENABLED));

    private final static boolean ARRAYLIST_POOLING_ENABLED = Boolean.parseBoolean(
        System.getProperty("org.multiverse.stm.beta.BetaObjectPool.arrayListPooling",""+ENABLED));

    private final static boolean CALLABLENODE_POOLING_ENABLED = Boolean.parseBoolean(
        System.getProperty("org.multiverse.stm.beta.BetaObjectPool.callableNodePooling",""+ENABLED));

    private final boolean tranlocalPoolingEnabled;
    private final boolean tranlocalArrayPoolingEnabled;
    private final boolean latchPoolingEnabled;
    private final boolean listenersPoolingEnabled;
    private final boolean listenersArrayPoolingEnabled;
    private final boolean arrayListPoolingEnabled;
    private final boolean callableNodePoolingEnabled;

    private final RefTranlocal[] tranlocalsBetaRef = new RefTranlocal[100];
    private int lastUsedBetaRef = -1;
    private final IntRefTranlocal[] tranlocalsBetaIntRef = new IntRefTranlocal[100];
    private int lastUsedBetaIntRef = -1;
    private final BooleanRefTranlocal[] tranlocalsBetaBooleanRef = new BooleanRefTranlocal[100];
    private int lastUsedBetaBooleanRef = -1;
    private final DoubleRefTranlocal[] tranlocalsBetaDoubleRef = new DoubleRefTranlocal[100];
    private int lastUsedBetaDoubleRef = -1;
    private final LongRefTranlocal[] tranlocalsBetaLongRef = new LongRefTranlocal[100];
    private int lastUsedBetaLongRef = -1;
    private TranlocalPool[] pools = new TranlocalPool[100];

    private CheapLatch[] cheapLatchPool = new CheapLatch[10];
    private int cheapLatchPoolIndex = -1;

    private StandardLatch[] standardLatchPool = new StandardLatch[10];
    private int standardLatchPoolIndex = -1;

    private Listeners[] listenersPool = new Listeners[100];
    private int listenersPoolIndex = -1;

    private ArrayList[] arrayListPool = new ArrayList[10];
    private int arrayListPoolIndex = -1;

    private CallableNode[] callableNodePool = new CallableNode[10];
    private int callableNodePoolIndex = -1;

    public BetaObjectPool() {
        arrayListPoolingEnabled = ARRAYLIST_POOLING_ENABLED;
        tranlocalArrayPoolingEnabled = TRANLOCALARRAY_POOLING_ENABLED;
        tranlocalPoolingEnabled = TRANLOCAL_POOLING_ENABLED;
        latchPoolingEnabled = LATCH_POOLING_ENABLED;
        listenersPoolingEnabled = LISTENER_POOLING_ENABLED;
        listenersArrayPoolingEnabled = LISTENERSARRAY_POOLING_ENABLED;
        callableNodePoolingEnabled = CALLABLENODE_POOLING_ENABLED;
    }

    /**
     * Takes a RefTranlocal from the pool for the specified BetaRef.
     *
     * @param owner the BetaRef to get the RefTranlocal for.
     * @return the pooled tranlocal, or null if none is found.
     * @throws NullPointerException if owner is null.
     */
    public RefTranlocal take(final BetaRef owner) {
        if (owner == null) {
            throw new NullPointerException();
        }

        if (!tranlocalPoolingEnabled) {
            return null;
        }

        if (lastUsedBetaRef == -1) {
            return null;
        }

        RefTranlocal tranlocal = tranlocalsBetaRef[lastUsedBetaRef];
        tranlocal.owner = owner;
        tranlocalsBetaRef[lastUsedBetaRef] = null;
        lastUsedBetaRef--;
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
        if (!tranlocalPoolingEnabled) {
            return;
        }

        if (lastUsedBetaRef == tranlocalsBetaRef.length - 1) {
            return;
        }

        tranlocal.prepareForPooling(this);
        lastUsedBetaRef++;
        tranlocalsBetaRef[lastUsedBetaRef] = tranlocal;
    }

    /**
     * Takes a IntRefTranlocal from the pool for the specified BetaIntRef.
     *
     * @param owner the BetaIntRef to get the IntRefTranlocal for.
     * @return the pooled tranlocal, or null if none is found.
     * @throws NullPointerException if owner is null.
     */
    public IntRefTranlocal take(final BetaIntRef owner) {
        if (owner == null) {
            throw new NullPointerException();
        }

        if (!tranlocalPoolingEnabled) {
            return null;
        }

        if (lastUsedBetaIntRef == -1) {
            return null;
        }

        IntRefTranlocal tranlocal = tranlocalsBetaIntRef[lastUsedBetaIntRef];
        tranlocal.owner = owner;
        tranlocalsBetaIntRef[lastUsedBetaIntRef] = null;
        lastUsedBetaIntRef--;
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
        if (!tranlocalPoolingEnabled) {
            return;
        }

        if (lastUsedBetaIntRef == tranlocalsBetaIntRef.length - 1) {
            return;
        }

        tranlocal.prepareForPooling(this);
        lastUsedBetaIntRef++;
        tranlocalsBetaIntRef[lastUsedBetaIntRef] = tranlocal;
    }

    /**
     * Takes a BooleanRefTranlocal from the pool for the specified BetaBooleanRef.
     *
     * @param owner the BetaBooleanRef to get the BooleanRefTranlocal for.
     * @return the pooled tranlocal, or null if none is found.
     * @throws NullPointerException if owner is null.
     */
    public BooleanRefTranlocal take(final BetaBooleanRef owner) {
        if (owner == null) {
            throw new NullPointerException();
        }

        if (!tranlocalPoolingEnabled) {
            return null;
        }

        if (lastUsedBetaBooleanRef == -1) {
            return null;
        }

        BooleanRefTranlocal tranlocal = tranlocalsBetaBooleanRef[lastUsedBetaBooleanRef];
        tranlocal.owner = owner;
        tranlocalsBetaBooleanRef[lastUsedBetaBooleanRef] = null;
        lastUsedBetaBooleanRef--;
        return tranlocal;
    }

    /**
     * Puts an old BooleanRefTranlocal in this pool. If the tranlocal is allowed to be null,
     * the call is ignored. The same goes for when the tranlocal is permanent, since you
     * can't now how many transactions are still using it.
     *
     * @param tranlocal the BooleanRefTranlocal to pool.
     */
    public void put(final BooleanRefTranlocal tranlocal) {
        if (!tranlocalPoolingEnabled) {
            return;
        }

        if (lastUsedBetaBooleanRef == tranlocalsBetaBooleanRef.length - 1) {
            return;
        }

        tranlocal.prepareForPooling(this);
        lastUsedBetaBooleanRef++;
        tranlocalsBetaBooleanRef[lastUsedBetaBooleanRef] = tranlocal;
    }

    /**
     * Takes a DoubleRefTranlocal from the pool for the specified BetaDoubleRef.
     *
     * @param owner the BetaDoubleRef to get the DoubleRefTranlocal for.
     * @return the pooled tranlocal, or null if none is found.
     * @throws NullPointerException if owner is null.
     */
    public DoubleRefTranlocal take(final BetaDoubleRef owner) {
        if (owner == null) {
            throw new NullPointerException();
        }

        if (!tranlocalPoolingEnabled) {
            return null;
        }

        if (lastUsedBetaDoubleRef == -1) {
            return null;
        }

        DoubleRefTranlocal tranlocal = tranlocalsBetaDoubleRef[lastUsedBetaDoubleRef];
        tranlocal.owner = owner;
        tranlocalsBetaDoubleRef[lastUsedBetaDoubleRef] = null;
        lastUsedBetaDoubleRef--;
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
        if (!tranlocalPoolingEnabled) {
            return;
        }

        if (lastUsedBetaDoubleRef == tranlocalsBetaDoubleRef.length - 1) {
            return;
        }

        tranlocal.prepareForPooling(this);
        lastUsedBetaDoubleRef++;
        tranlocalsBetaDoubleRef[lastUsedBetaDoubleRef] = tranlocal;
    }

    /**
     * Takes a LongRefTranlocal from the pool for the specified BetaLongRef.
     *
     * @param owner the BetaLongRef to get the LongRefTranlocal for.
     * @return the pooled tranlocal, or null if none is found.
     * @throws NullPointerException if owner is null.
     */
    public LongRefTranlocal take(final BetaLongRef owner) {
        if (owner == null) {
            throw new NullPointerException();
        }

        if (!tranlocalPoolingEnabled) {
            return null;
        }

        if (lastUsedBetaLongRef == -1) {
            return null;
        }

        LongRefTranlocal tranlocal = tranlocalsBetaLongRef[lastUsedBetaLongRef];
        tranlocal.owner = owner;
        tranlocalsBetaLongRef[lastUsedBetaLongRef] = null;
        lastUsedBetaLongRef--;
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
        if (!tranlocalPoolingEnabled) {
            return;
        }

        if (lastUsedBetaLongRef == tranlocalsBetaLongRef.length - 1) {
            return;
        }

        tranlocal.prepareForPooling(this);
        lastUsedBetaLongRef++;
        tranlocalsBetaLongRef[lastUsedBetaLongRef] = tranlocal;
    }

    public Tranlocal take(final BetaTransactionalObject owner) {
        if (owner == null) {
            throw new NullPointerException();
        }

        if(!tranlocalPoolingEnabled){
            return null;
        }

        int classIndex = owner.___getClassIndex();

        if(classIndex == -1){
            return null;
        }

        switch(classIndex){
            case 0:
                return take((BetaRef)owner);
            case 1:
                return take((BetaIntRef)owner);
            case 2:
                return take((BetaBooleanRef)owner);
            case 3:
                return take((BetaDoubleRef)owner);
            case 4:
                return take((BetaLongRef)owner);
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

    /**
     * Puts a Tranlocal in the pool.
     *
     */
    public void put(final Tranlocal tranlocal) {
        if (!tranlocalPoolingEnabled || tranlocal == null) {
            return;
        }

        BetaTransactionalObject owner = tranlocal.owner;
        int classIndex = owner.___getClassIndex();

        if(classIndex == -1){
            return;
        }

        switch(classIndex){
            case 0:
                put((RefTranlocal)tranlocal);
                return;
            case 1:
                put((IntRefTranlocal)tranlocal);
                return;
            case 2:
                put((BooleanRefTranlocal)tranlocal);
                return;
            case 3:
                put((DoubleRefTranlocal)tranlocal);
                return;
            case 4:
                put((LongRefTranlocal)tranlocal);
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

        tranlocal.prepareForPooling(this);
        pool.lastUsed++;
        pool.tranlocals[pool.lastUsed] = tranlocal;
    }

    static class TranlocalPool{
        int lastUsed = -1;
        Tranlocal[] tranlocals = new Tranlocal[100];
    }

    private Tranlocal[][] tranlocalArrayPool = new Tranlocal[8193][];

    /**
     * Puts a Tranlocal array in the pool.
     *
     * @param array the Tranlocal array to put in the pool.
     * @throws NullPointerException is array is null.
     */
    public void putTranlocalArray(final Tranlocal[] array){
        if(array == null){
            throw new NullPointerException();
        }

        if(!tranlocalArrayPoolingEnabled){
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
        for(int k=0;k < array.length;k++){
            array[k]=null;
        }

        tranlocalArrayPool[index]=array;
    }

    /**
     * Takes a tranlocal array from the pool with the given size.
     *
     * @param size the size of the array to take
     * @return the Tranlocal array taken from the pool, or null if none available.
     * @throws IllegalArgumentException if size smaller than 0.
     */
    public Tranlocal[] takeTranlocalArray(final int size){
        if(size<0){
            throw new IllegalArgumentException();
        }

        if(!tranlocalArrayPoolingEnabled){
            return null;
        }

        int index = size;

        if(index >= tranlocalArrayPool.length){
            return null;
        }

        if(tranlocalArrayPool[index]==null){
            return null;
        }

        Tranlocal[] array = tranlocalArrayPool[index];
        tranlocalArrayPool[index]=null;
        return array;
    }

  /**
     * Takes a CallableNode from the pool, or null if none is available.
     *
     * @return the CallableNode from the pool, or null if none available.
     */
    public CallableNode takeCallableNode(){
        if(!callableNodePoolingEnabled || callableNodePoolIndex == -1){
            return null;
        }

        CallableNode node = callableNodePool[callableNodePoolIndex];
        callableNodePool[callableNodePoolIndex]=null;
        callableNodePoolIndex--;
        return node;
    }

    /**
     * Puts a CallableNode in the pool.
     *
     * @param node the CallableNode to pool.
     * @throws NullPointerException if node is null.
     */
    public void putCallableNode(CallableNode node){
        if(node == null){
            throw new NullPointerException();
        }

        if(!callableNodePoolingEnabled || callableNodePoolIndex == callableNodePool.length-1){
            return;
        }

        node.prepareForPooling();
        callableNodePoolIndex++;
        callableNodePool[callableNodePoolIndex]=node;
    }

    /**
     * Takes a CheapLatch from the pool, or null if none is available.
     *
     * @return the CheapLatch from the pool, or null if none available.
     */
    public CheapLatch takeCheapLatch(){
        if(!latchPoolingEnabled || cheapLatchPoolIndex == -1){
            return null;
        }

        CheapLatch latch = cheapLatchPool[cheapLatchPoolIndex];
        cheapLatchPool[cheapLatchPoolIndex]=null;
        cheapLatchPoolIndex--;
        return latch;
    }

    /**
     * Puts a CheapLatch in the pool. Before the latch is put in the pool, it is prepared for pooling.
     *
     * @param latch the CheapLatch to pool.
     * @throws NullPointerException if latch is null.
     */
    public void putCheapLatch(CheapLatch latch){
        if(latch == null){
            throw new NullPointerException();
        }

        if(!latchPoolingEnabled || cheapLatchPoolIndex == cheapLatchPool.length-1){
            return;
        }

        latch.prepareForPooling();
        cheapLatchPoolIndex++;
        cheapLatchPool[cheapLatchPoolIndex]=latch;
    }

    /**
     * Takes a StandardLatch from the pool.
     *
     * @return the taken StandardLatch is null if none is available.
     */
    public StandardLatch takeStandardLatch(){
        if(!latchPoolingEnabled || standardLatchPoolIndex == -1){
            return null;
        }

        StandardLatch latch = standardLatchPool[standardLatchPoolIndex];
        standardLatchPool[standardLatchPoolIndex]=null;
        standardLatchPoolIndex--;
        return latch;
    }

    /**
     * Puts a StandardLatch in the pool. The latch is prepared for pooling before being placed in the pool.
     *
     * @param latch the StandardLatch to pool.
     * @throws NullPointerException if latch is null.
     */
    public void putStandardLatch(StandardLatch latch){
        if(latch == null){
            throw new NullPointerException();
        }

        if(!latchPoolingEnabled || standardLatchPoolIndex == standardLatchPool.length-1){
            return;
        }

        latch.prepareForPooling();
        standardLatchPoolIndex++;
        standardLatchPool[standardLatchPoolIndex]=latch;
    }

    // ====================== array list ===================================

    /**
     * Takes an ArrayList from the pool, The returned ArrayList is cleared.
     *
     * @return the ArrayList from the pool, or null of none is found.
     */
    public ArrayList takeArrayList(){
        if(!arrayListPoolingEnabled || arrayListPoolIndex == -1){
            return null;
        }

        ArrayList list = arrayListPool[arrayListPoolIndex];
        arrayListPool[arrayListPoolIndex]=null;
        arrayListPoolIndex--;
        return list;
    }

    /**
     * Puts an ArrayList in this pool. The ArrayList will be cleared before being placed
     * in the pool.
     *
     * @param list the ArrayList to place in the pool.
     * @throws NullPointerException if list is null.
     */
    public void putArrayList(ArrayList list){
        if(list == null){
            throw new NullPointerException();
        }

        if(!arrayListPoolingEnabled || arrayListPoolIndex == arrayListPool.length-1){
            return;
        }

        list.clear();
        arrayListPoolIndex++;
        arrayListPool[arrayListPoolIndex]=list;
    }


    // ============================ listeners ==================================

    /**
     * Takes a Listeners object from the pool.
     *
     * @return the Listeners object taken from the pool. or null if none is taken.
     */
    public Listeners takeListeners(){
        if(!listenersPoolingEnabled || listenersPoolIndex == -1){
            return null;
        }

        Listeners listeners = listenersPool[listenersPoolIndex];
        listenersPool[listenersPoolIndex]=null;
        listenersPoolIndex--;
        return listeners;
    }

    /**
     * Puts a Listeners object in the pool. The Listeners object is preparedForPooling before
     * it is put in the pool. The next Listeners object is ignored (the next field itself is ignored).
     *
     * @param listeners the Listeners object to pool.
     * @throws NullPointerException is listeners is null.
     */
    public void putListeners(Listeners listeners){
        if(listeners == null){
            throw new NullPointerException();
        }

        if(!listenersPoolingEnabled || listenersPoolIndex == listenersPool.length-1){
            return;
        }

        listeners.prepareForPooling();
        listenersPoolIndex++;
        listenersPool[listenersPoolIndex]=listeners;
    }

    // ============================= listeners array =============================

    private Listeners[] listenersArray = new Listeners[100000];

    /**
     * Takes a Listeners array from the pool. If an array is returned, it is completely nulled.
     *
     * @param minimalSize the minimalSize of the Listeners array.
     * @return the found Listeners array, or null if none is taken from the pool.
     * @throws IllegalArgumentException if minimalSize is smaller than 0.
     */
    public Listeners[] takeListenersArray(int minimalSize){
        if( minimalSize < 0 ){
            throw new IllegalArgumentException();
        }

        if(!listenersArrayPoolingEnabled){
            return null;
        }

        if(listenersArray == null || listenersArray.length < minimalSize){
            return null;
        }

        Listeners[] result = listenersArray;
        listenersArray = null;
        return result;
    }

    /**
     * Puts a Listeners array in the pool.
     *
     * Listeners array should be nulled before being put in the pool. It is not going to be done by this
     * BetaObjectPool but should be done when the listeners on the listeners array are notified.
     *
     * @param listenersArray the array to pool.
     * @throws NullPointerException if listenersArray is null.
     */
    public void putListenersArray(Listeners[] listenersArray){
        if(listenersArray == null){
            throw new NullPointerException();
        }

        if(!listenersArrayPoolingEnabled){
            return;
        }

        if(this.listenersArray!=listenersArray){
            return;
        }

        this.listenersArray = listenersArray;
    }
}
